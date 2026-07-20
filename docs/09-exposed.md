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

> 注: 以下は Exposed DSL の最小例。現在のコードは書き込み=`MessageRepository` /
> 読み取り=`MessageQueryService` に分離し（CQRS・[21](./21-ddd-cqrs-structure.md)）、
> テーブルは `TMessage`（uuid 主キー・[26](./26-uuidv7-primary-key.md)）。

### 1. テーブル定義（`TMessage.kt`）

```kotlin
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object TMessage : Table("t_message") {
    // id は DB の DEFAULT uuidv7()（PG18 ネイティブ）で採番 → databaseGenerated()
    val id = uuid("id").databaseGenerated()
    val userId = uuid("user_id")   // FK 制約は Flyway 側が持つ（ここは素の列として写す）
    val content = text("content")
    // created_at / updated_at は DB の DEFAULT now() で入る。INSERT 時に触らせない
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()
    val updatedAt = timestampWithTimeZone("updated_at").databaseGenerated()
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

### ⑤ `uuid()` 列は `kotlin.uuid.Uuid`（`java.util.UUID` ではない）

Exposed 1.x の `uuid("...")` 列が扱う型は、`java.util.UUID` ではなく Kotlin ネイティブの
**`kotlin.uuid.Uuid`（experimental）**。訓練データの古い Exposed 感覚（java.util.UUID）だと
`Argument type mismatch: actual type is 'Uuid', but 'UUID' was expected` でハマる。

方針は「**正準型は `java.util.UUID`**」（OpenAPI 生成物も java.util.UUID・ドメインに experimental を
漏らさない）。`kotlin.uuid.Uuid` は **Exposed アダプタ内に封じ込め**、境界で相互変換する。

```kotlin
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

// 読み取り: kotlin.uuid.Uuid → java.util.UUID
@OptIn(ExperimentalUuidApi::class)
private fun ResultRow.toMessage() =
    Message(id = MessageId(this[TMessage.id].toJavaUuid()), /* ... */)

// 書き込み: java.util.UUID → kotlin.uuid.Uuid
it[TMessage.userId] = userId.value.toKotlinUuid()
```

`@OptIn(ExperimentalUuidApi::class)` は変換を行う infra の関数だけに付ければよい（domain には不要）。
主キー移行の全体像は [26-uuidv7-primary-key.md](./26-uuidv7-primary-key.md)。

## 参考リンク

- [Exposed 公式ドキュメント](https://www.jetbrains.com/help/exposed/home.html)
- [Spring Boot integration](https://www.jetbrains.com/help/exposed/spring-boot-integration.html)
