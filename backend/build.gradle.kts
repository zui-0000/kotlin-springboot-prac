import org.flywaydb.gradle.FlywayExtension

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        // Flyway を独立コマンド(./gradlew flywayMigrate)で実行するための構成。
        // プラグイン本体・DB モジュール・JDBC ドライバを同じ buildscript クラスパスに
        // 同居させることで、プラグインがドライバを確実に見つけられる（別だと "No database found"）。
        classpath("org.flywaydb:flyway-gradle-plugin:12.4.0")
        classpath("org.flywaydb:flyway-database-postgresql:12.4.0")
        classpath("org.postgresql:postgresql:42.7.11")
    }
}

plugins {
    // Kotlin 本体（JVM 向け）
    kotlin("jvm") version "2.4.0"
    // Spring 用に all-open を適用（@Component などのクラスを自動で open に）
    kotlin("plugin.spring") version "2.4.0"
    // Spring Boot 本体
    id("org.springframework.boot") version "4.1.0"
    // Spring Boot の BOM でライブラリのバージョンを一元管理
    id("io.spring.dependency-management") version "1.1.7"
    // ktlint（Lint / フォーマット）
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    // OpenAPI からサーバの API interface / DTO を生成（スキーマ駆動）
    id("org.openapi.generator") version "7.14.0"
}

// Flyway プラグインは buildscript 経由で適用（ドライバと同じクラスパスに乗せるため）
apply(plugin = "org.flywaydb.flyway")

group = "com.example"
version = "0.0.1-SNAPSHOT"

kotlin {
    // Java 25 (Temurin, mise で管理) を使用
    jvmToolchain(25)
    compilerOptions {
        // Kotlin の null 安全を JSR-305 アノテーションにも厳格適用
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

repositories {
    mavenCentral()
}

val kotestVersion = "5.9.1"
val exposedVersion = "1.3.1"

dependencies {
    // --- Spring Boot ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    // OpenAPI 生成コードが使う @Valid / バリデーション制約（jakarta.validation）
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // Exposed が使う DataSource（HikariCP）の自動設定を提供
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    // Kotlin を JSON 変換・リフレクションで扱うために必要
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // --- ORM (Exposed) ---
    // JetBrains 製の Kotlin ネイティブ ORM。Boot4 用スターター。
    // core / jdbc / dao / spring-transaction を推移的に取り込む。
    implementation("org.jetbrains.exposed:exposed-spring-boot4-starter:$exposedVersion")
    // java.time 型（timestampWithTimeZone → OffsetDateTime）のサポート
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    // --- DB / マイグレーション ---
    // Spring Boot 4 はオートコンフィグがモジュール分割された。
    // flyway-core だけでは Flyway が起動時に走らないため、この配線モジュールが必須。
    implementation("org.springframework.boot:spring-boot-flyway")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // --- テスト ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Kotest 本体（JUnit5 ランナー経由で実行）
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    // Kotest から Spring のテスト機能を使う拡張
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.3.0")
}

// Flyway Gradle プラグインの接続設定。
// ※ このプラグインは application.yml を読まないため、ここで接続先を指定する。
//    値は docker-compose.yml の直書き値に合わせ、環境変数があればそれを優先する。
configure<FlywayExtension> {
    val port = System.getenv("POSTGRES_PORT") ?: "5432"
    val db = System.getenv("POSTGRES_DB") ?: "prac"
    url = "jdbc:postgresql://localhost:$port/$db"
    user = System.getenv("POSTGRES_USER") ?: "prac"
    password = System.getenv("POSTGRES_PASSWORD") ?: "pracpass"
    // アプリと同じマイグレーション置き場を参照
    locations = arrayOf("filesystem:src/main/resources/db/migration")
    // application.yml と揃える（タイムスタンプ方式のため）
    outOfOrder = true
}

tasks.withType<Test> {
    // Kotest は JUnit Platform 上で動くため必須
    useJUnitPlatform()
}

tasks.processResources {
    // schema.sql（参照用スナップショット）と dump スクリプトは実行時に不要なので jar に含めない。
    // ※ db/migration 配下は Flyway が実行時に読むため除外しない。
    exclude("db/schema.sql", "db/dump-schema.sh")
}

// --- OpenAPI コード生成（スキーマ駆動） ---
// schema/openapi.yaml から API interface と DTO を生成する。
// interfaceOnly: Controller は自前実装（Spring Boot 版への依存を減らし堅牢化）。
openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set("$rootDir/schema/openapi.yaml")
    // 生成物は build/ の外・プロジェクト直下の generated/ に出す（見つけやすく・非コミット）。
    outputDir.set("$projectDir/generated/openapi")
    apiPackage.set("com.example.prac.generated.api")
    modelPackage.set("com.example.prac.generated.model")
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true", // jakarta を使う。Boot4 も jakarta なので整合する
            "documentationProvider" to "none", // swagger 依存を増やさない
            "useTags" to "true", // tag ごとに API interface を分ける
            "dateLibrary" to "java8", // java.time を使う
        ),
    )
}

// $ref で分割した spec 断片（refs/**・model/**）も入力として追跡する。
// openApiGenerate は既定では inputSpec(openapi.yaml)しか監視しないため、$ref 先の
// paths.yaml / model/*.yaml "だけ" を編集すると UP-TO-DATE 判定で再生成が走らない。
// schema/ 配下すべてを入力に加えることで、どの断片を触っても再生成されるようにする。
tasks.named<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerate") {
    inputs
        .dir("$rootDir/schema")
        .withPropertyName("openapiSpecFiles")
        .withPathSensitivity(org.gradle.api.tasks.PathSensitivity.RELATIVE)
}

// OpenAPI 仕様の妥当性を検証する（./gradlew openApiValidate / mise run schema-validate）
openApiValidate {
    inputSpec.set("$rootDir/schema/openapi.yaml")
    recommend.set(true) // 構文チェックに加えてベストプラクティスの推奨も出す
}

// 生成された Kotlin を main のソースに含める
sourceSets.main {
    kotlin.srcDir("$projectDir/generated/openapi/src/main/kotlin")
}

// build/ の外（generated/）に生成するため、clean 時に明示的に削除する
tasks.named<Delete>("clean") {
    delete("$projectDir/generated")
}

// コンパイル前に必ず生成する
tasks.named("compileKotlin") {
    dependsOn("openApiGenerate")
}

// 生成コードは ktlint の対象外にする
ktlint {
    filter {
        exclude { it.file.path.contains("generated/openapi") }
    }
}

// ktlint は main ソースセット（生成コードのディレクトリを含む）を走査するため、
// 生成タスクへの依存を明示する（未宣言だと Gradle がタスク順序の問題として検出する）。
tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask>().configureEach {
    dependsOn("openApiGenerate")
}
