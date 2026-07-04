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

## mise 設定とコマンドの実行場所（重要）

mise 設定は2階層に分割している。

| 場所 | ツール | タスク |
|------|--------|--------|
| ルート `mise.toml` | `lefthook` / `committed` | `hooks-install` |
| `backend/mise.toml` | `java` / `gradle` | `dev` / `test` / `fix` / `build` / `generate` / `schema-validate` / `db-*` |

- **backend のタスクは `backend/` 内で実行**する（`cd backend && mise run <task>`）。
  タスクは定義元の config ディレクトリ（backend/）を CWD として動くため、`dir` 指定は不要。
- mise は親子の config をマージする: **backend からは親（ルート）のタスク（`hooks-install`）も見える**。
  逆に**ルートからは backend タスクは見えない**（＝「backend の操作は backend から」を実現）。
- Git フック（lefthook）はリポジトリのルートで動くため、backend タスクを呼ぶ箇所は
  `cd backend && mise run ...` としている。

## エディタからタスクを実行する（.vscode/tasks.json）

mise 拡張はワークスペースのルートを基準に mise を解決するため、backend/mise.toml の
タスクを UI（run ボタン / タスクツリー）に出せない（ルートからは backend タスクが見えないため）。
マルチルートワークスペースも試したが、拡張が子フォルダの config を拾わなかった。

代替として **`.vscode/tasks.json`（ルート）** に backend タスクを定義している。
- コマンドパレット（`Cmd+Shift+P`）→ **「Tasks: Run Task」** で一覧・実行できる。
- 直前のタスク再実行は「Tasks: Rerun Last Task」。
- 各タスクは `cwd = ${workspaceFolder}/backend` で実行されるため、リポジトリのルートを
  普通に開くだけでよい（マルチルート不要）。
- もちろんターミナルから `cd backend && mise run <task>` でも実行できる。

## 注意
- `backend/` へ移動する前に書かれた一部の docs では、パスを `src/...` のように
  `backend/` を省略して記載している場合がある。実際の場所は `backend/src/...`。
