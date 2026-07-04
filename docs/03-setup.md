# セットアップ & 実行手順

> 前提: mise / Docker がインストール済みであること。

## 初回セットアップ

```bash
# 1. ツール（JDK25 / Gradle9.6.1）を導入
mise install

# 2. PostgreSQL を起動（接続情報は docker-compose.yml に直書き済み）
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

## Gradle Wrapper (`gradlew`) とは

`gradlew` は **Gradle Wrapper**。Gradle 本体が未インストールでも、
指定バージョンの Gradle を自動 DL してビルドできる「代理実行スクリプト」。

- `gradle-wrapper.properties` に書かれた版（9.6.1）を自動取得して実行する。
- OS 別に2ファイルある:
  - `gradlew` … macOS / Linux 用（シェルスクリプト）
  - `gradlew.bat` … Windows 用（バッチファイル）
- 生成コマンド（初回だけ / 手で書くものではない）:
  ```bash
  gradle wrapper --gradle-version 9.6.1
  ```
  → `gradlew` / `gradlew.bat` / `gradle/wrapper/*` の4ファイルが生成され、Git 管理する。

## mise と Gradle Wrapper の役割分担（両方必要）

「mise を入れたなら wrapper は不要では？」と思いがちだが、**役割が違うので両方必要**。

| 対象 | mise が管理 | wrapper が管理 |
|------|:---:|:---:|
| **Java (JDK25)** | ✅ | ❌ できない |
| **Gradle のバージョン** | ✅ | ✅ |

### ポイント

- **wrapper は Java を用意できない。** Gradle を DL するだけで、それを動かす JVM は別途必要。
  その JDK を用意するのが **mise**（wrapper には絶対できない仕事）。
- **Gradle の版指定は mise と wrapper で重複している。** それでも wrapper を残すのは、
  wrapper が**業界標準で「mise 無しの相手」にも通じる**から:
  - CI/CD は `./gradlew build` を叩く前提で動く
  - IDE（IntelliJ / Cursor 拡張）は勝手に `gradlew` を見つけて使う
  - mise 未導入のチームメンバーでも `./gradlew` なら動く

### 結論

| | 役割 |
|---|------|
| **mise** | **Java を用意** + ローカルのツール統一 |
| **gradlew** | Gradle 版を固定・**誰の環境でも通じる** |

→ 「mise があるから wrapper 不要」ではなく、
　**mise が Java を、wrapper が Gradle を、それぞれ得意分野で支える二人三脚**。
　日常のビルドは `./gradlew` を使うのが基本。
