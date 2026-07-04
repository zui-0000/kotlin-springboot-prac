# kotlin-springboot-prac

Kotlin + Spring Boot によるサーバサイド開発の**学習用プロジェクト**。
2026年時点の最新スタックで構成し、「なぜそうするか」を [`docs/`](./docs) に記録しながら進めている。

## 技術スタック

| 分類 | 採用 | バージョン |
|------|------|-----------|
| 言語 | Kotlin | 2.4.0 |
| JDK | Java (Temurin) | 25 (LTS) |
| フレームワーク | Spring Boot | 4.1.0 |
| ビルド | Gradle (Kotlin DSL) | 9.6.1 |
| DB | PostgreSQL | 18 |
| ORM | Exposed | 1.3.1 |
| マイグレーション | Flyway | (Boot 管理) |
| Lint/Format | ktlint | 14.2.0 |
| テスト | Kotest | 5.9.1 |
| ツール管理 | mise | - |

> 選定理由の詳細は [docs/01-tech-stack.md](./docs/01-tech-stack.md) を参照。

## 必要なもの

- [mise](https://mise.jdx.dev/)（JDK / Gradle のバージョン管理）
- Docker / Docker Compose（ローカル PostgreSQL 用）

## セットアップ

```bash
# 1. ツール（JDK25 / Gradle9.6.1）を導入
mise install

# 2. Git フックを有効化（コミット前チェック・メッセージ規約。clone後1回）
mise run hooks-install

# 3. PostgreSQL を起動（接続情報は docker-compose.yml に直書き済み）
mise run db-up
```

> コミットメッセージは Conventional Commits（`feat:` `fix:` `docs:` …）に従う。
> 詳細は [docs/17-git-hooks.md](./docs/17-git-hooks.md)。

## 起動

```bash
# アプリ起動（local プロファイル固定・Flyway がマイグレーションを流してから起動する）
mise run dev
```

起動後の動作確認:

```bash
curl localhost:8080/hello
curl -X POST localhost:8080/messages -H 'Content-Type: application/json' -d '{"content":"hello"}'
curl localhost:8080/messages
```

## コマンド一覧

よく使う操作は **mise のタスク**にまとめてある（`mise run <名前>`、`mise <名前>` でも可）。
一覧は `mise tasks ls`。mise 経由なので JDK25 が有効な状態で実行される。

| コマンド | 内容 | 実体 |
|----------|------|------|
| `mise run dev` | マイグレーション+`schema.sql`更新の後にアプリ起動（local固定・SQL ログ有り） | `db-migrate` → `bootRun` |
| `mise run test` | テスト実行（Kotest） | `./gradlew test` |
| `mise run format` | Kotlin コードを自動整形 | `./gradlew ktlintFormat` |
| `mise run lint` | Lint チェック | `./gradlew ktlintCheck` |
| `mise run fix` | 整形してから残りをチェック（一発） | `ktlintFormat` → `ktlintCheck` |
| `mise run build` | ビルド（テスト・Lint 含む） | `./gradlew build` |
| `mise run generate` | OpenAPI から API interface/DTO を生成 | `./gradlew openApiGenerate` |
| `mise run db-up` | PostgreSQL 起動 | `docker compose up -d` |
| `mise run db-stop` | PostgreSQL を止める（コンテナは残す） | `docker compose stop` |
| `mise run db-reset` | PostgreSQL をデータごと作り直す | `docker compose down -v && up -d` |
| `mise run db-migrate` | マイグレーション実行 + `schema.sql` 更新（アプリ起動なし） | `flywayMigrate` → `dump-schema.sh` |
| `mise run hooks-install` | Git フックを有効化（clone 後1回） | `lefthook install` |

## DB マイグレーションのやり方

スキーマ変更は **Flyway のマイグレーションファイルを追加**して行う（既存ファイルは編集しない）。

1. `src/main/resources/db/migration/` に新しい SQL を追加。
   ファイル名は **タイムスタンプ方式**: `V<YYYYMMDDHHMMSS>__<説明>.sql`
   ```
   例: V20260705093000__add_title_to_messages.sql
   ```
   ```sql
   ALTER TABLE messages ADD COLUMN title VARCHAR(100);
   ```
2. 必要なら Exposed の `Table` 定義（`Messages.kt`）も合わせて更新。
3. `mise run dev` または `mise run db-migrate` を実行する。
   どちらも「**マイグレーション適用 → `schema.sql` 更新**」を行う（`dev` はその後アプリを起動する）。
   → `schema.sql` は自動生成されるので、手で編集しないこと（先頭に警告ヘッダあり）。

> 詳細は [docs/11-flyway-migrations.md](./docs/11-flyway-migrations.md) /
> [docs/12-current-schema-visibility.md](./docs/12-current-schema-visibility.md)。

## プロジェクト構成（モノレポ）

リポジトリ全体は「バックエンド + インフラ + スキーマ」の3本柱で構成する。

```
kotlin-springboot-prac/
├── README.md / CLAUDE.md / mise.toml   # リポジトリ全体の設定・方針
├── lefthook.yml / committed.toml       # Git フック・コミット規約（全体）
├── docs/                               # 学習ノート（全体）
├── backend/                            # Kotlin / Spring Boot アプリ
├── schema/                             # OpenAPI 定義（API 契約・コード生成の起点）
└── infrastructures/                    # Terraform（クラウドインフラ）
```

`backend/` の中身:

```
backend/
├── build.gradle.kts / settings.gradle.kts / gradlew
├── docker-compose.yml        # ローカル PostgreSQL（接続情報を直書き）
└── src/
    ├── main/
    │   ├── kotlin/com/example/prac/
    │   │   ├── PracApplication.kt        # エントリポイント
    │   │   ├── HelloController.kt        # 動作確認用エンドポイント
    │   │   └── message/                  # message 機能
    │   │       ├── Messages.kt           # Exposed の Table 定義
    │   │       ├── Message.kt            # ドメイン/レスポンス
    │   │       ├── MessageService.kt     # DSL でDB読み書き（@Transactional）
    │   │       └── MessageController.kt  # REST エンドポイント
    │   └── resources/
    │       ├── application.yml / application-local.yml
    │       └── db/
    │           ├── migration/            # Flyway マイグレーション
    │           ├── schema.sql            # 現在スキーマの参照用スナップショット（自動生成）
    │           └── dump-schema.sh        # schema.sql 生成スクリプト
    └── test/kotlin/com/example/prac/     # Kotest によるテスト
```

> コマンドは `mise run <task>` に統一。アプリ系タスクは自動で `backend/` 内で実行される
> （`mise.toml` の各タスクに `dir = "backend"` を設定）。詳細は
> [docs/18-repository-structure.md](./docs/18-repository-structure.md)。

## ドキュメント

学習の過程で得た知識・判断根拠を [`docs/`](./docs) に蓄積している。
一覧は [docs/README.md](./docs/README.md) を参照。
