# CLAUDE.md

このリポジトリで作業する際のプロジェクト方針。
詳細な背景・判断根拠は `docs/` に蓄積しているので、迷ったらまず `docs/README.md` を見る。

## プロジェクト概要

Kotlin + Spring Boot のサーバサイド開発を学ぶための **学習用プロジェクト**。
2026年時点の最新スタックで構成している。

- 言語: Kotlin 2.4 / JDK 25 (Temurin, mise 管理)
- FW: Spring Boot 4.1 / ビルド: Gradle (Kotlin DSL) 9.6.1
- DB: PostgreSQL 18 (Docker) / ORM: Exposed 1.3.1 / マイグレーション: Flyway
- テスト: Kotest 5.9.1 / Lint・整形: ktlint

## リポジトリ構成（モノレポ）

- `backend/` … Kotlin/Spring アプリ一式（`build.gradle.kts` / `src/`、および API 契約 `schema/openapi.yaml`）
- `infrastructures/` … Terraform（クラウドインフラ）
- ※ フロントを持たず backend が唯一の消費者のため、OpenAPI 契約は `backend/schema/` 配下に置く
- ルート … `mise.toml` / `lefthook.yml` / `committed.toml` / `docs/`（全体設定）
- 詳細は [docs/18-repository-structure.md](docs/18-repository-structure.md)。

## コマンドは mise タスク経由で実行する

mise 設定は2階層:
- **ルート `mise.toml`**: `lefthook` / `committed`（ツール）+ `hooks-install`（タスク）。
- **`backend/mise.toml`**: `java` / `gradle`（ツール）+ アプリ系タスク（`dev` / `test` / `fix` /
  `build` / `generate` / `schema-validate` / `db-*`）。

**backend のタスクは `backend/` 内で実行する**（`cd backend && mise run <task>`）。
タスクは定義元の config ディレクトリ（backend/）を CWD として動くため、`dir` 指定は不要。
ルートからは backend タスクは見えない（backend からは親の `hooks-install` も見える）。

注意: 素の `./gradlew` は `backend/` で、かつシェルで mise が有効な前提（Java 25 を使うため）。
mise が有効でない非対話環境では `mise exec -- ./gradlew ...`（`backend/` で）を使う。

## アーキテクチャ

`Controller → Service(@Transactional) → Exposed の Table` の3層。
`backend/src/main/kotlin/com/example/prac/message/` が実例。

## 重要な決定（勝手に元へ戻さないこと）

- **ORM は Exposed**（JPA ではない）。Kotlin ネイティブを重視して移行済み。
  - パッケージは `org.jetbrains.exposed.v1.*`。`eq` 等の演算子は **トップレベル関数**を import する。
- **`schema.sql` は自動生成物**（先頭に編集禁止ヘッダあり）。手で編集しない。
  `db-migrate` / `dev` の実行時に再生成される。Git 追跡は継続する（PR でスキーマ差分をレビューするため）。
- Flyway のマイグレーションは **タイムスタンプ命名** `V<YYYYMMDDHHMMSS>__説明.sql`。
  **適用済みファイルは絶対に編集しない**（変更は新規ファイルを追加する）。
- ローカル DB の認証情報は `docker-compose.yml` に直書き（秘密ではない）。**`.env` は使わない**。
  本番は AWS Secret Manager 等から環境変数で注入する想定で、`application.yml` の
  `${VAR:デフォルト}` がそれを受ける。
- Spring Boot 4 はオートコンフィグがモジュール分割された。Flyway には `spring-boot-flyway` が必要
  （`flyway-core` だけでは起動時に走らない）。
- **API はスキーマ駆動**。`backend/schema/openapi.yaml` が契約の正。openapi-generator で
  `backend/build/generated/` に interface/DTO を生成し、Controller がそれを実装する。
  生成コードは gitignore（コミットしない）。API 変更は openapi.yaml を編集して再生成。
  詳細は [docs/19-openapi-codegen.md](docs/19-openapi-codegen.md)。

## 作業の進め方（このプロジェクトの流儀）

- **最新スタックのため、依存追加や API は推測せず裏取りする**（Maven Central / 公式ドキュメント / web 検索）。
  訓練データより新しい可能性が高く、実際に何度もハマっている（`docs/04-troubleshooting.md`）。
- **「なぜそうするか」を重視する学習プロジェクト**。ユーザーの疑問に答えたら、本人が希望すれば
  `docs/NN-*.md`（連番）として残し、`docs/README.md` の目次を更新する。
- コミット / プッシュは **ユーザーが依頼したときだけ** 行う。
  コミットメッセージは日本語、末尾に `Co-Authored-By` を付ける。
- **コミットメッセージは Conventional Commits 必須**。Git フック（lefthook + committed）で強制される。
  - 形式: `<type>(<scope 任意>): <説明>`（scope は自由・省略可、文字数制限なし）
  - type（11種）: `feat` `fix` `docs` `style` `refactor` `perf` `test` `chore` `build` `ci` `revert`
  - 末尾に ASCII 句読点（`.` 等）を付けない。`fixup!` / `WIP` は不可。
  - 完全なルールは `committed.toml` と [docs/17-git-hooks.md](docs/17-git-hooks.md)。
  - 非対話環境から commit する場合は、フックが lefthook/committed を見つけられるよう
    `mise exec -- git commit ...` で実行する。フック未導入なら `mise run hooks-install`。
- cSpell（スペルチェッカ）が技術用語（`prac` / `ktlint` / `kotest` 等）に出す警告は無害。無視してよい。
