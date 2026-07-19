# OpenAPI コード生成（スキーマ駆動）

> `backend/schema/openapi.yaml`（API 契約）から Kotlin の API interface / DTO を生成する仕組み。

## 全体像

```
backend/schema/openapi.yaml      … API 契約の入口（$ref を束ねる司令塔・手書き）
backend/schema/refs/<feature>/   … 機能ごとに分割した契約の実体（paths.yaml + model/）
      │ openapi-generator（kotlin-spring）
      ▼
backend/generated/openapi/       … MessagesApi(interface) + DTO（自動生成・build/ の外）
      │
backend の Controller が生成 interface を実装
```

- **契約優先（contract-first）**: 先に API 仕様を決め、それに従って実装する。
- 契約は**機能ごとにファイル分割**している（下記「spec のファイル分割」）。`openapi.yaml` は入口だけ。
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
    inputSpec.set("$rootDir/schema/openapi.yaml")   // 入口の openapi.yaml を指定
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

// $ref 先の分割ファイルも入力として追跡する（詳細は「ハマりどころ」の "再生成されない罠"）。
tasks.named<GenerateTask>("openApiGenerate") {
    inputs.dir("$rootDir/schema").withPropertyName("openapiSpecFiles")
        .withPathSensitivity(org.gradle.api.tasks.PathSensitivity.RELATIVE)
}

sourceSets.main { kotlin.srcDir("$projectDir/generated/openapi/src/main/kotlin") }  // 生成物を main に含める
tasks.named("compileKotlin") { dependsOn("openApiGenerate") }           // コンパイル前に生成
tasks.named<Delete>("clean") { delete("$projectDir/generated") }        // build/ の外なので clean で明示削除
ktlint { filter { exclude { it.file.path.contains("generated/openapi") } } }  // 生成物は lint 対象外
```

## Controller は生成 interface を実装する

```kotlin
@RestController
class MessageController(
    private val createMessageCommandHandler: CreateMessageCommandHandler,
    private val listMessagesQueryHandler: ListMessagesQueryHandler,
) : MessagesApi {
    override fun listMessages(): ResponseEntity<List<MessageResponse>> = ...
    override fun createMessage(req: CreateMessageRequest): ResponseEntity<MessageResponse> = ...
    // 生成モデル(com.example.prac.generated.model.Message)は MessageResponse として import している
}
```

- ルーティング（`@RequestMapping` 等）と入出力の型は**生成 interface が持つ**。
- Controller は「生成 Request → Command/Query 変換」「Handler(= ユースケース) 呼び出し」
  「結果 DTO → 生成レスポンス変換」だけを担う（CQRS の 4 層構成。[21](./21-ddd-cqrs-structure.md)）。
- `override` が必須な理由・ルーティングの紐づけ方は [25](./25-spring-ioc-di.md) と Controller の KDoc を参照。

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

## spec のファイル分割（`$ref`・機能で割る）

`openapi.yaml` 一枚に全部を書かず、**機能ごとにファイル分割**している。`openapi.yaml` は
`$ref` を束ねる入口（司令塔）だけにし、実体は `refs/<feature>/` に閉じる（「機能で割る」の延長）。

```
schema/
  openapi.yaml              # 入口: info / servers / paths(=$ref) だけ
  refs/
    messages/
      paths.yaml            # collection(/messages) + member(/messages/{id})
      model/
        Message.yaml
        CreateMessageRequest.yaml
        UpdateMessageRequest.yaml
```

```yaml
# openapi.yaml — paths は各機能の path item を $ref で指すだけ
paths:
  /messages:
    $ref: "./refs/messages/paths.yaml#/collection"
  /messages/{id}:
    $ref: "./refs/messages/paths.yaml#/member"
```

### なぜ path が2エントリ必要か（1つの `$ref` にできない）

`paths` は「URL 文字列 → Path Item」のマップ（TS の `Record<string, PathItem>`）。`/messages` と
`/messages/{id}` は**別 URL = 別キー**なので、エントリも2つ要る。`$ref` を置けるのは各 Path Item の
位置までで、**`paths` マップ本体は `$ref` 非対応**（マップ全体を1参照で差し替えることはできない）。
エントリ数 = URL の数、であってファイル分割の都合ではない。

### 1機能 = 1 paths.yaml（JSON ポインタで path item を持つ）

機能の path を**1ファイルに集約**するため、`paths.yaml` の中に path item を**キー付きで**並べ、
`openapi.yaml` から JSON ポインタ（`#/collection`・`#/member`）で参照する。

```yaml
# refs/messages/paths.yaml
collection:            # /messages       … コレクション・リソース（一覧・登録）
  get: { operationId: listMessages, ... }
  post: { operationId: createMessage, ... }
member:                # /messages/{id}  … メンバー・リソース（取得・更新・削除）
  parameters:          # {id} は配下の get/put/delete で共有
    - { name: id, in: path, required: true, schema: { type: integer, format: int64 } }
  get: { operationId: getMessage, ... }
  put: { operationId: updateMessage, ... }
  delete: { operationId: deleteMessage, ... }
```

**規約**: キー名は REST の語彙で **`collection`（複数の集まり）/ `member`（その1要素）**に統一する。
これは Rails の "collection routes / member routes" と同じ対で、全機能でこの2キーに揃える。

- **`item` ではなく `member`** を採用（`member` が REST の正確な用語）。
- **キーを URL 文字列にしない**（`/messages` をキーにすると JSON ポインタで `/` を `~1` に
  エスケープする必要が出て `#/~1messages~1{id}` のように読めなくなる）。短い英単語キーにする。
- モデル参照は `paths.yaml` からの相対パス（`$ref: "./model/Message.yaml"`）。

### 別リソースを追加するとき

`refs/items/paths.yaml` と `refs/items/model/` を掘り、`openapi.yaml` に
`/items:` `/items/{id}:` の `$ref` を足すだけ。生成側は `useTags` により `ItemsApi` +
`ItemController` が1対1で対応する（上記「タグ設計」）。

> 外部ファイル `$ref` 分割・JSON ポインタ参照とも、生成結果（クラス名・`@RequestMapping`・
> `@PathVariable`）は一枚 spec と**完全に同一**であることを実測で確認済み。

## ハマりどころ・判断メモ

- **`$ref` 分割ファイルが再生成されない罠（重要）**: `openApiGenerate` は既定で
  `inputSpec`（= `openapi.yaml`）**しか** up-to-date 判定の入力にしない。そのため
  `openapi.yaml` を触らず `refs/**/paths.yaml` や `model/*.yaml` **だけ**を編集すると、
  Gradle が「変更なし」と判断して `UP-TO-DATE` で**再生成をスキップ**する
  （= コード変更が反映されず「なんで反映されないの?」にハマる）。
  対策として `openApiGenerate` の入力に **`schema/` ディレクトリ全体**を宣言している:

  ```kotlin
  tasks.named<GenerateTask>("openApiGenerate") {
      inputs.dir("$rootDir/schema").withPropertyName("openapiSpecFiles")
          .withPathSensitivity(org.gradle.api.tasks.PathSensitivity.RELATIVE)
  }
  ```

  これで分割ファイルのどれを編集しても再生成が走る。**単一ファイル spec なら踏まなかった、
  分割の代償**として発生する罠なので、`$ref` 分割とセットで必須の設定。
  （応急処置としては `./gradlew openApiGenerate --rerun-tasks` で強制再生成もできる）
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
