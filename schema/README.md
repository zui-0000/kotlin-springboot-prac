# schema/

API の契約（スキーマ）を定義する場所。**スキーマ駆動開発（contract-first）** の起点。

## 方針

- **OpenAPI** で REST API の仕様を定義する（`openapi.yaml` を正とする）。
- ここを起点にコードを生成する:
  - **バックエンド（Kotlin）**: サーバの API interface と DTO を生成し、Controller が実装する。
  - **（将来）フロントエンド（TS）**: API クライアントを生成する。

## 位置づけ

```
schema/openapi.yaml   ← API の契約（唯一の正）
      │ openapi-generator
      ├─→ backend  : Kotlin の API interface / DTO
      └─→ frontend : TS の API クライアント（将来）
```

## TODO（未実装）

- [ ] `openapi.yaml` を用意する
- [ ] backend 側に openapi-generator を組み込み、ビルド時にコード生成する
- [ ] 生成コードを Controller が実装する形に整える
