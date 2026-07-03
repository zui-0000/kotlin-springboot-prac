# Exposed（ORM）の使い方メモ

> JetBrains 製の Kotlin ネイティブ ORM。本プロジェクトでは JPA から移行して採用。
> バージョン: Exposed 1.3.1 / Spring Boot 4.1 用スターター使用。

## なぜ JPA から Exposed に移行したか

| 観点 | JPA/Hibernate | Exposed |
|------|--------------|---------|
| Kotlin らしさ | `open` 必須・data class 不可の制約 | 普通の `class` / `data class` でよい |
| SQL の透明性 | Hibernate が隠す（自動発行） | 型安全な DSL で発行 SQL が見える |
| 作り手 | 汎用（Java 由来） | **JetBrains（Kotlin と同じ会社）** |

「Kotlin らしく、SQL を意識しながら書きたい」という方針で Exposed を選択。

## 依存関係（build.gradle.kts）

```kotlin
val exposedVersion = "1.3.1"

// Boot4 用スターター（core / jdbc / dao / spring-transaction を推移的に含む）
implementation("org.jetbrains.exposed:exposed-spring-boot4-starter:$exposedVersion")
// java.time 型（timestampWithTimeZone → OffsetDateTime）
implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
// Exposed が使う DataSource(HikariCP) の自動設定
implementation("org.springframework.boot:spring-boot-starter-jdbc")
```

## 設定（application.yml）

```yaml
spring:
  exposed:
    # スキーマは Flyway が管理するため、Exposed の自動テーブル生成は無効化
    generate-ddl: false
```

> Flyway との関係は JPA 時代と同じ。「スキーマの正は Flyway、Exposed は合わせる側」。
> （[04-troubleshooting.md](./04-troubleshooting.md) の「スキーマの正は誰が持つか」参照）

## コードの構成

### 1. テーブル定義（`Messages.kt`）

```kotlin
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object Messages : Table("messages") {
    val id = long("id").autoIncrement()
    val content = text("content")
    // DB の DEFAULT now() で入る列。databaseGenerated() で INSERT 時に触らせない
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()
    override val primaryKey = PrimaryKey(id)
}
```

### 2. ドメイン/レスポンス（`Message.kt`）

```kotlin
// JPA と違い、ただの data class でよい
data class Message(val id: Long, val content: String, val createdAt: OffsetDateTime)
```

### 3. サービス（`MessageService.kt`）— DSL で読み書き

```kotlin
import org.jetbrains.exposed.v1.core.eq          // ← 後述: トップレベル関数
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

@Service
@Transactional   // Spring がトランザクションを開閉 → DSL を transaction {} で囲まなくてよい
class MessageService {
    fun list() = Messages.selectAll().map { it.toMessage() }

    fun create(content: String): Message {
        val newId = Messages.insert { it[Messages.content] = content }[Messages.id]
        return Messages.selectAll().where { Messages.id eq newId }.single().toMessage()
    }
}
```

## ハマりどころメモ

### ① クエリ DSL は実行時にトランザクションが必要
Exposed の DSL は必ずトランザクション内で実行する必要がある。
Spring Boot スターター + `@Transactional` を付ければ Spring が面倒を見てくれるので、
`transaction {}` ブロックで囲む必要はない（サービスのメソッドに `@Transactional` を付ける）。

### ② `eq` は「トップレベル関数」を import する（1.0 での変更）
`where { Messages.id eq newId }` の `eq` は、
Exposed 1.0 で `SqlExpressionBuilder` のメンバから**トップレベル関数へ移行**した。

```kotlin
import org.jetbrains.exposed.v1.core.eq   // これが正
// import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq  // 非推奨(deprecated)
```

（旧 API を import するとコンパイルエラー: "deprecated ... replaced with the equivalent top-level function"）

### ③ パッケージが `org.jetbrains.exposed.v1.*`（1.0 で再編）
- テーブル定義・型: `org.jetbrains.exposed.v1.core.*`
- DSL（insert / selectAll など）: `org.jetbrains.exposed.v1.jdbc.*`
- java.time 型: `org.jetbrains.exposed.v1.javatime.*`

### ④ DB 生成列は `databaseGenerated()`
`created_at` のように DB の DEFAULT で値が入る列は `.databaseGenerated()` を付ける。
これで Exposed が INSERT 文に含めず、DB のデフォルトが効く。
（JPA の `insertable = false` に相当）

> なお JPA 版では POST のレスポンスで `createdAt` が null になったが、
> Exposed 版は INSERT 後に読み直す実装にしたため、生成された値が返る。

## 参考リンク

- [Exposed 公式ドキュメント](https://www.jetbrains.com/help/exposed/home.html)
- [Spring Boot integration](https://www.jetbrains.com/help/exposed/spring-boot-integration.html)
