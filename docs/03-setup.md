# セットアップ & 実行手順

> 前提: mise / Docker がインストール済みであること。

## 初回セットアップ

```bash
# 1. ツール（JDK25 / Gradle9.6.1）を導入
mise install

# 2. 環境変数ファイルを用意
cp .env.example .env

# 3. PostgreSQL を起動（バックグラウンド）
docker compose up -d
```

## アプリの起動

```bash
# Flyway がマイグレーションを流し、その後アプリが起動する
./gradlew bootRun
```

> mise のシェルフックが有効なら `./gradlew` はそのまま Java 25 で動く。
> もし `Cannot find a Java installation ... matching languageVersion=25` と出たら、
> mise が有効化されていない証拠。`mise exec -- ./gradlew bootRun` で回避できる。

起動後、別ターミナルで動作確認:

```bash
curl localhost:8080/hello
curl -X POST localhost:8080/messages -H 'Content-Type: application/json' -d '{"content":"hello"}'
curl localhost:8080/messages
```

## よく使うコマンド

| コマンド | 内容 |
|----------|------|
| `./gradlew bootRun` | アプリ起動 |
| `./gradlew test` | テスト実行（Kotest） |
| `./gradlew ktlintCheck` | Lint チェック |
| `./gradlew ktlintFormat` | 自動整形 |
| `./gradlew build` | ビルド（テスト・Lint 含む） |
| `docker compose up -d` | PostgreSQL 起動 |
| `docker compose down` | PostgreSQL 停止（データは残る） |
| `docker compose down -v` | データごと削除 |

## DB に直接つなぐ

```bash
docker exec -it prac-postgres psql -U prac -d prac
# 例: \dt でテーブル一覧、SELECT * FROM messages;
```
