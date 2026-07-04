# リポジトリ全体の構成（モノレポ）

> リポジトリを「バックエンド + インフラ + スキーマ」に分けたモノレポ構成のメモ。
> ※ このプロジェクトの Kotlin パッケージ構成は [14-project-structure-and-packages.md](./14-project-structure-and-packages.md)。

## トップレベル構成

```
kotlin-springboot-prac/
├── README.md / CLAUDE.md / mise.toml   # リポジトリ全体の設定・方針
├── lefthook.yml / committed.toml       # Git フック・コミット規約（リポジトリ全体）
├── docs/                               # 学習ノート（全体）
├── backend/                            # Kotlin / Spring Boot アプリ
├── schema/                             # OpenAPI 定義（API 契約・コード生成の起点）
└── infrastructures/                    # Terraform（クラウドインフラ）
```

## 3本柱の役割

| ディレクトリ | 中身 | 補足 |
|--------------|------|------|
| `backend/` | Kotlin/Spring アプリ | `build.gradle.kts` や `src/` など、アプリ一式がここ |
| `schema/` | OpenAPI 仕様 | API の契約（唯一の正）。ここから backend/frontend のコードを生成 |
| `infrastructures/` | Terraform | クラウドインフラを IaC で管理。アプリとは独立 |

- リポジトリ全体に関わる設定（`mise.toml` / `lefthook.yml` / `committed.toml` / `docs/`）は
  ルートに置く。
- スキーマ駆動開発（contract-first）: `schema/openapi.yaml` を起点に、
  backend の API interface / DTO を生成する（将来はフロントの TS クライアントも）。

## なぜアプリを backend/ に移したか
- Terraform（`infrastructures/`）や OpenAPI（`schema/`）と**同じリポジトリで並べて管理**するため。
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
