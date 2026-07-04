# schema/

API の契約（スキーマ）を定義する場所。**スキーマ駆動開発（contract-first）** の起点。

## 方針

- **OpenAPI**（`openapi.yaml`）で REST API の仕様を定義し、これを唯一の正とする。
- ここを起点に **backend の Kotlin コード（API interface / DTO）を生成**する。

## 流れ（実装済み）

```
schema/openapi.yaml
      │ ./gradlew openApiGenerate（openapi-generator / kotlin-spring）
      ▼
backend/build/generated/openapi/  … MessagesApi(interface) + DTO（自動生成・gitignore対象）
      │
backend の MessageController が MessagesApi を実装
```

- 生成は `mise run generate`（または backend の `./gradlew openApiGenerate`）。
- `mise run build` / `dev` でも、コンパイル前に自動生成される。
- 生成コードは `build/` 配下（gitignore 対象）。コミットしない。

## 使い方（API を変えるとき）

1. `openapi.yaml` を編集する（契約を変更）。
2. `mise run generate` で再生成、または `mise run build` / `dev`。
3. 生成された interface の変更に合わせて Controller の実装を直す。

詳細は [../docs/19-openapi-codegen.md](../docs/19-openapi-codegen.md)。
