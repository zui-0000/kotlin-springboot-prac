# infrastructures/

クラウドインフラを **Terraform** で管理する場所（Infrastructure as Code）。

## 方針

- 本番・検証環境のインフラ（AWS 等）を Terraform で定義する。
- アプリのコード（`backend/`）とは独立して管理する。

## 想定する構成（例）

```
infrastructures/
├── environments/
│   ├── dev/       ← 検証環境
│   └── prod/      ← 本番環境
├── modules/       ← 再利用する Terraform モジュール
└── ...
```

## TODO（未実装）

- [ ] Terraform の初期化（provider 設定・state 管理方針）
- [ ] mise で terraform のバージョンを管理する（`mise.toml` の `[tools]`）
- [ ] 環境ごとのディレクトリ / モジュールを整備する

> 補足: DB の接続情報など秘密情報は AWS Secret Manager 等で管理し、
> アプリには環境変数で注入する想定（[../docs/10-application-config.md](../docs/10-application-config.md)）。
