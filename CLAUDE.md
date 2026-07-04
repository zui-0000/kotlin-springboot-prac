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

## コマンドは mise タスク経由で実行する

操作は `mise run <task>`（一覧は `mise tasks ls`）。主なもの:

- `mise run dev` … マイグレーション + schema.sql 更新の後、アプリ起動（local プロファイル固定）
- `mise run test` / `mise run fix`（整形→lint） / `mise run build`
- `mise run db-up` / `db-stop` / `db-reset` / `db-migrate`（マイグレーション + schema.sql 更新）

注意: 素の `./gradlew` はシェルで mise が有効な前提（Java 25 を使うため）。
mise が有効でない非対話環境では `mise exec -- ./gradlew ...` を使う。

## アーキテクチャ

`Controller → Service(@Transactional) → Exposed の Table` の3層。
`src/main/kotlin/com/example/prac/message/` が実例。

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
