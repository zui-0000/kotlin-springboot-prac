# リポジトリ全体の構成（モノレポ）

> リポジトリを「バックエンド + インフラ + スキーマ」に分けたモノレポ構成のメモ。
> ※ このプロジェクトの Kotlin パッケージ構成は [14-project-structure-and-packages.md](./14-project-structure-and-packages.md)。

## トップレベル構成

```
kotlin-springboot-prac/
├── README.md / CLAUDE.md / mise.toml   # リポジトリ全体の設定・方針
├── lefthook.yml / committed.toml       # Git フック・コミット規約（リポジトリ全体）
├── docs/                               # 学習ノート（全体）
├── backend/                            # Kotlin / Spring Boot アプリ（API 契約 schema/ も配下）
└── infrastructures/                    # Terraform（クラウドインフラ）
```

## 役割

| ディレクトリ | 中身 | 補足 |
|--------------|------|------|
| `backend/` | Kotlin/Spring アプリ | `build.gradle.kts` / `src/`、API 契約 `schema/openapi.yaml` を含む |
| `infrastructures/` | Terraform | クラウドインフラを IaC で管理。アプリとは独立 |

- リポジトリ全体に関わる設定（`mise.toml` / `lefthook.yml` / `committed.toml` / `docs/`）は
  ルートに置く。
- スキーマ駆動開発（contract-first）: `backend/schema/openapi.yaml` を起点に、
  backend の API interface / DTO を生成する（[19-openapi-codegen.md](./19-openapi-codegen.md)）。

## なぜ schema を backend 配下に置くか
- **API 契約の消費者が backend だけ**（フロントを持たない）ため、契約は backend の持ち物。
- 契約を複数コンポーネントで共有する場合はルート直下に独立させる意味があるが、
  単一消費者ではその必要がなく、`backend/schema/` に置く方が所有関係が明確でパスも単純。

## なぜアプリを backend/ に移したか
- Terraform（`infrastructures/`）と**同じリポジトリで並べて管理**するため。
- アプリをルート直下に置いたままだと、リポジトリ全体のファイルと混在して見通しが悪い。
- `backend/` に隔離することで、各コンポーネントの責務が明確になる。

## コマンドの実行場所（重要）
- 操作は `mise run <task>` に統一。
- アプリ系タスク（`dev` / `test` / `build` / `db-*` など）は **`mise.toml` で `dir = "backend"`**
  を指定しており、自動で `backend/` 内で実行される。手動で `cd backend` する必要はない。
- `hooks-install`（lefthook）はリポジトリのルートで動く（Git フックはリポジトリ全体のため）。

## 注意
- `backend/` へ移動する前に書かれた一部の docs では、パスを `src/...` のように
  `backend/` を省略して記載している場合がある。実際の場所は `backend/src/...`。
