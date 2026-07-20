# API レスポンスのエンベロープ設計

> `message` の OpenAPI 契約を、[typespec-schema-template](https://github.com/zui-0000/typespec-schema-template)
> （zui の TypeSpec 製スキーマ規約テンプレート）の構成へ移植した記録。2026-07-20。
> テンプレは TypeSpec だが本プロジェクトは OpenAPI YAML なので、記法ではなく **思想と構成** を移植した。

## 構成（refs の中身）

```
schema/refs/
  common/                       … 横断共有型（複数リソースで共有）
    model/{Uuid, CreatedAt, UpdatedAt}
    meta/{RespondedAt, CommonResponseMeta}
    pagination/{CurrentPage, PerPage, TotalCount, TotalPages}
    error/{ErrorCode, ErrorMessage, ErrorDetail, BadRequestError, NotFoundError}
  messages/                     … per-operation 構成
    Message{Create,Get,List}{Request,Response,Result}.yaml
    MessageListPagination.yaml   MessageUpdateRequest.yaml
    model/MessageContent.yaml    … リソース固有スカラー（制約付き）
    paths.yaml
```

`refs/` 自体は変えず、`refs/<domain>/` の中身をこの規約に揃える。共有型は `refs/common/`、
リソース固有のスカラーは各 `refs/<domain>/model/` に置く。

## 命名: `{Domain}{Action}{Type}`

| Type | 役割 | 例 |
|---|---|---|
| **Request** | API への入力ボディ | `MessageCreateRequest` |
| **Result** | データ本体（Response の中身） | `MessageGetResult` |
| **Response** | `result` / `meta` [/ `pagination`] を束ねるラッパー | `MessageGetResponse` |

## エンベロープパターン

成功レスポンスは「データ本体」と「メタ情報」を分離する。

```jsonc
// 単一
{ "result": { ...message... }, "meta": { "respondedAt": "..." } }
// 一覧
{ "result": [ ... ], "meta": { "respondedAt": "..." },
  "pagination": { "totalCount": 100, "totalPages": 10, "currentPage": 1, "perPage": 10 } }
```

- **`data` でなく `result`**: フロント（TanStack Query）がレスポンス全体を `data` で包むため、
  フィールドも `data` だと `data.data` になり冗長。`result` にして `data.result` で読めるようにする。
- **`meta` と `pagination` を分ける**: `meta` は「レスポンス自体の情報（いつ返したか）」、
  `pagination` は「データセットの情報（何件・何ページ）」で性質が異なる。分けておくと
  ページネーション不要な endpoint で `pagination` ごと省ける。
- **エラーは `error` で包まずフラット**: HTTP ステータスが既にエラーを表すので二重に包まない。
  `{ errorCode, message, meta }`。`errorCode` は先頭3桁が HTTP ステータス。**500 は本文なし**。

## ステータス対応（テンプレの `main.tsp` を契約の正とした）

| op | ステータス | ボディ |
|---|---|---|
| list | 200 | `MessageListResponse`（result[] + meta + pagination） / 500 |
| get | 200 | `MessageGetResponse`（result + meta） / 404 / 500 |
| create | 201 | `MessageCreateResponse`（result + meta） / 400 / 500 |
| update | 204 | 本文なし / 400 / 404 / 500 |
| delete | 204 | 本文なし / 404 / 500 |

- **create は `result + meta`** にした（作成した id を返す方が有用）。テンプレ本体は meta-only だが、
  ここは意図的に逸脱している。

## フロントが無いのに、なぜエンベロープ？（トレードオフ）

`result` / `meta` の分離は本来**フロント前提**の設計（だから本プロジェクトは TypeSpec でなく
生 OpenAPI を選んだ経緯がある。[19-openapi-codegen.md](./19-openapi-codegen.md)）。今はフロントが無い。
それでも採用したのは **学習目的 + zui の既存テンプレとの規約統一**のため。
「フロントレスなバックエンドには過剰かも」という自覚は持った上での選択。

## Kotlin 側の波及

エンベロープは契約の型を変える（`List<Message>` → `MessageListResponse`）ので Kotlin に波及する。

- **Controller** が DTO を生成レスポンス型（`MessageListResponse` 等）へ包み、`meta.respondedAt` を付与する
  （`respondedAt` はレスポンス生成時の関心事なので presentation 層で付ける）。
- **pagination は Handler で実装**: `currentPage`/`perPage`（既定 1/10）→ offset 計算 →
  `listPaged(limit, offset)` + `count()` → `totalPages` 算出。application 層の責務。
- **未実装（宣言のみ）**: get/update/delete の本体、エラーの `@RestControllerAdvice`（400/404 を実際に返す配線）。
  契約には宣言済みで、実装は認証・エラーハンドリング回に回す。

## 関連

- [19-openapi-codegen.md](./19-openapi-codegen.md) … OpenAPI 分割・`$ref`・collection/member 規約
- [21-ddd-cqrs-structure.md](./21-ddd-cqrs-structure.md) … Controller → Handler → Repository/QueryService
