# OpenAPI コード生成（スキーマ駆動）

> `backend/schema/openapi.yaml`（API 契約）から Kotlin の API interface / DTO を生成する仕組み。

## 全体像

```
backend/schema/openapi.yaml  … API の契約（唯一の正・手書き）
      │ openapi-generator（kotlin-spring）
      ▼
backend/generated/openapi/  … MessagesApi(interface) + DTO（自動生成・build/ の外）
      │
backend の Controller が生成 interface を実装
```

- **契約優先（contract-first）**: 先に API 仕様を決め、それに従って実装する。
- 生成コードは `backend/generated/`（**gitignore 対象**）。ビルドごとに再生成するのでコミットしない。
  build/ の外に置くのは「生成ソースは見つけやすい場所に・コンパイル物だけ build/ に」という分離のため
  （`build/` はコンパイル結果 = クラス/jar だけになる）。
  （`schema.sql` は追跡するが、こちらは追跡しない。役割が違う）

## なぜ OpenAPI（生 YAML）で、TypeSpec ではないか
- 現状は「単一の Kotlin バックエンド・フロントなし・Node を避ける」方針。
  TypeSpec は Node が必要で、TypeSpec→OpenAPI→Kotlin の2段になる。
- 生 OpenAPI なら Node 不要・1段。契約優先の目的は十分達成できる。
- 将来フロント（TS）が増える・API が大規模化するなら TypeSpec 移行も容易
  （TypeSpec は同じ OpenAPI を吐くだけ）。

## build.gradle.kts の設定（要点）

```kotlin
plugins {
    id("org.openapi.generator") version "7.14.0"
}

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set("$rootDir/schema/openapi.yaml")   // backend/schema/ を参照
    outputDir.set("$projectDir/generated/openapi")   // build/ の外・gitignore 対象
    apiPackage.set("com.example.prac.generated.api")
    modelPackage.set("com.example.prac.generated.model")
    configOptions.set(mapOf(
        "interfaceOnly" to "true",     // Controller は自前実装
        "useSpringBoot3" to "true",    // jakarta（Boot4 も jakarta）
        "documentationProvider" to "none",
        "useTags" to "true",
        "dateLibrary" to "java8",      // java.time
    ))
}

sourceSets.main { kotlin.srcDir("$projectDir/generated/openapi/src/main/kotlin") }  // 生成物を main に含める
tasks.named("compileKotlin") { dependsOn("openApiGenerate") }           // コンパイル前に生成
tasks.named<Delete>("clean") { delete("$projectDir/generated") }        // build/ の外なので clean で明示削除
ktlint { filter { exclude { it.file.path.contains("generated/openapi") } } }  // 生成物は lint 対象外
```

## Controller は生成 interface を実装する

```kotlin
@RestController
class MessageController(private val service: MessageService) : MessagesApi {
    override fun listMessages(): ResponseEntity<List<MessageDto>> = ...
    override fun createMessage(req: CreateMessageRequest): ResponseEntity<MessageDto> = ...
}
```

- ルーティング（`@RequestMapping` 等）と入出力の型は**生成 interface が持つ**。
- Controller はサービス呼び出しと「ドメイン ⇔ 生成 DTO」の変換だけを担う。

## エンドポイントが増えるとどうなるか（タグ設計）

`configOptions` の **`useTags = true`** により、operation を **tag ごとにグループ化して
1タグ = 1インターフェース**を生成する。つまり構造はこう:

```
openapi.yaml の tag  =  生成インターフェース  =  Controller
   tags: [messages]  →     MessagesApi       →  MessageController
   tags: [items]     →     ItemsApi          →  ItemController
```

**1タグ = 1 Api インターフェース = 1 Controller** が、リソース単位で1対1に対応する。
これは DDD の「機能で割る（1リソース=1 Controller）」と噛み合う（[21](./21-ddd-cqrs-structure.md)）。

### 増え方は2パターン

- **同じリソースにエンドポイント追加（同じ tag）** → 既存インターフェースにメソッドが増える。
  例: `messages` に `GET /messages/{id}` を足すと `MessagesApi` に `getMessage()` が増え、
  `MessageController` がそれを `override` するだけ。インターフェースは1つのまま。
- **新しいリソース追加（新しい tag）** → 新インターフェース + 新 Controller。
  例: `tags: [items]` を付けた `/items` を書くと `ItemsApi` が生成され、
  `item/presentation/ItemController : ItemsApi` を実装する。

### 規約：1機能（境界づけられたコンテキスト）= 1タグ

タグをバラバラに付けると生成インターフェースが変な粒度で割れる。
**`message` は `messages` タグ、`item` は `items` タグ**、と機能パッケージと1対1で揃える。
こうすると「生成インターフェース ↔ Controller ↔ 機能パッケージ」が綺麗に並ぶ。

### spec が巨大化したら `$ref` でファイル分割

`openapi.yaml` 一枚が読みにくくなったら、パス定義やスキーマを外部ファイルへ切り出せる:

```yaml
paths:
  /messages:
    $ref: "./paths/messages.yaml"
  /items:
    $ref: "./paths/items.yaml"
```

**正は `openapi.yaml` のまま**、中身を機能単位に分割する（「機能で割る」の延長）。今は1リソースなので不要。

## ハマりどころ・判断メモ

- **Spring Boot 4 対応**: openapi-generator の kotlin-spring は Boot4 対応が新しめ。
  `interfaceOnly = true` + `useSpringBoot3 = true`（jakarta）にすることで、
  Boot 版に依存しにくい形（interface + DTO のみ）で生成し、Boot 4.1 でコンパイル・動作を確認済み。
- **`spring-boot-starter-validation` が必要**: 生成コードが `@Valid` を使うため依存に追加。
- **Gradle プラグイン版**: 本体（openapi-generator）は 7.2x まであるが、
  Gradle プラグインの最新は 7.14.0（本体より遅れる）。プラグインは 7.14.0 を使用。

## 仕様の検証（validate）
- `mise run schema-validate`（= `./gradlew openApiValidate`）で `openapi.yaml` の妥当性を検証する。
  新ツールは不要（openapi-generator プラグインに同梱）。`recommend = true` でベストプラクティスも助言。
- `schema/*.yaml` を変更してコミットすると、**pre-commit フックが自動で検証**する
  （壊れた仕様はコミットできない）。[17-git-hooks.md](./17-git-hooks.md) 参照。

## コマンド
- `mise run schema-validate` … 仕様の妥当性を検証（`./gradlew openApiValidate`）
- `mise run generate` … 生成のみ（`./gradlew openApiGenerate`）
- `mise run build` / `mise run dev` … コンパイル前に自動生成される
