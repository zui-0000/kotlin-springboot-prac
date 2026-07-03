# 技術選定と方針

> 最終更新: 2026-07-03
> 目的: Kotlin / Spring Boot によるサーバサイド開発の学習

## この文書について

学習用プロジェクトの技術選定と、その選定理由をまとめる。
「なぜこれを選んだか」を残すことで、後から振り返って学びにつなげるのが目的。

## 採用スタック（2026年7月時点の最新を基本に選定）

| 分類 | 採用 | バージョン | 備考 |
|------|------|-----------|------|
| 言語 | Kotlin | 2.4.0 | サーバサイド Kotlin の学習が目的 |
| JDK | Java (Temurin) | 25 (LTS) | 2025/9 の LTS。Spring Boot 4 は Java 17〜26 対応 |
| フレームワーク | Spring Boot | 4.1.0 | 2026/6/10 リリース。Spring Framework 7 ベース |
| ビルドツール | Gradle (Kotlin DSL) | 9.6.1 | ビルドスクリプトも Kotlin で書ける |
| DB | PostgreSQL | (Docker で最新安定版) | ローカルはコンテナで起動 |
| データアクセス | Spring Data JPA | (Spring Boot 管理) | Entity / Repository で DB を読み書き |
| マイグレーション | Flyway | (Spring Boot 管理) | DB スキーマのバージョン管理 |
| Linter/Formatter | ktlint-gradle | 14.2.0 | `ktlintCheck` / `ktlintFormat` タスク |
| テスト | Kotest | 5.9.1 | 安定版。6.0 はまだ Milestone のため見送り |
| ツール管理 | mise | (最新) | JDK / Gradle のバージョンをピン留め |
| コンテナ | Docker / Docker Compose | - | ローカル DB の起動、アプリのコンテナ化 |

## 選定理由（判断のポイント）

### JDK は Java 25 (LTS)
- 今から始めるなら最新 LTS が合理的。Spring Boot 4 は Java 17〜26 に対応しているため 25 で問題なし。
- mise でピン留めするので、既存の Homebrew 版 Java 17 とは独立して管理できる。

### Spring Boot は 4.1（最新を優先）
- 「最新を使いたい」という方針を尊重して 4.1 を採用。
- 注意点: 2026/6 リリースで出たてのため、世の中のチュートリアルや Q&A はまだ 3.x が多数。情報を探すときは 4.x / Spring Framework 7 での差分に注意する。

### テストは Kotest 5.9.1（安定版を優先）
- Kotest 6.0 は 2026/7 時点でまだ Milestone（正式版ではない）。
- 学習中に「フレームワークのバグか自分のミスか」で消耗しないよう、安定版の 5.9.1 を採用。

### マイグレーションは Flyway（Liquibase ではなく）
- PostgreSQL 単体構成なら Flyway で十分。SQL をそのまま書けるので直感的で学習向き。
- Liquibase は多DB対応・高度なロールバックなどが必要になったら検討する。

### ツール管理は mise（SDKMAN ではなく）
- `mise.toml` に JDK / Gradle のバージョンを書いて `mise install` で一括セットアップ。
- SDKMAN よりシェル起動が速く（約10ms vs 300ms）、複数言語を一元管理できる。
- プロジェクトに入った瞬間に正しい JDK に切り替わるため、再現性が高い。

## ディレクトリ構成（予定）

```
kotlin-springboot-prac/
├── mise.toml                # JDK25 + Gradle9.6.1 をピン留め
├── build.gradle.kts         # Kotlin DSL のビルドスクリプト
├── settings.gradle.kts
├── docker-compose.yml       # PostgreSQL をローカル起動
├── Dockerfile               # アプリのコンテナ化
├── docs/                    # ← このドキュメント群
├── src/
│   ├── main/
│   │   ├── kotlin/          # Application / Controller / Entity / Repository
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/    # Flyway の V1__init.sql など
│   └── test/
│       └── kotlin/          # Kotest によるテスト
└── ...
```

## 処理の流れ（全体像）

```
[SQL ファイルを書く (db/migration/)]
        ↓
[アプリ起動 (bootRun)] → Flyway が未適用の SQL を PostgreSQL に流す
        ↓
[Spring Data JPA] が出来上がったテーブルを Kotlin から読み書き
```

各レイヤーの役割:
- PostgreSQL … データを保存する「箱」
- Flyway ……… 箱の「形（スキーマ）」を管理（マイグレーション、**実行時**に動く）
- Spring Data JPA … 箱の「中身」を読み書き（データ操作）
- ktlint ……… Kotlin コードの見た目を整える（開発ツール、**ビルド時/手動**）

## 参考リンク

- [Spring Boot リリース情報](https://github.com/spring-projects/spring-boot/releases)
- [Kotlin リリース情報](https://kotlinlang.org/docs/releases.html)
- [Gradle リリース情報](https://gradle.org/releases/)
- [Kotest 公式](https://kotest.io/)
- [Flyway 公式](https://documentation.red-gate.com/flyway)
- [mise 公式](https://mise.jdx.dev/)
