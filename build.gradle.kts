plugins {
    // Kotlin 本体（JVM 向け）
    kotlin("jvm") version "2.4.0"
    // Spring 用に all-open を適用（@Component などのクラスを自動で open に）
    kotlin("plugin.spring") version "2.4.0"
    // JPA エンティティ用に no-arg コンストラクタを自動生成
    kotlin("plugin.jpa") version "2.4.0"
    // Spring Boot 本体
    id("org.springframework.boot") version "4.1.0"
    // Spring Boot の BOM でライブラリのバージョンを一元管理
    id("io.spring.dependency-management") version "1.1.7"
    // ktlint（Lint / フォーマット）
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

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

dependencies {
    // --- Spring Boot ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // Kotlin を JSON 変換・リフレクションで扱うために必要
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

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

tasks.withType<Test> {
    // Kotest は JUnit Platform 上で動くため必須
    useJUnitPlatform()
}
