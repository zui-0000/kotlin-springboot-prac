# application.yml と設定・プロファイル

> Spring Boot の設定ファイル `application.yml` の仕組みと、
> 環境ごとに設定を切り替える「プロファイル」のメモ。

## application.yml とは

Spring Boot アプリの**設定ファイル**。DB 接続先・サーバーポート・ライブラリの挙動などを、
コードを書き換えずに外から調整する。置き場所は `src/main/resources/application.yml`
（Spring が起動時にここを自動で読む）。

> TS で例えると `.env` と `config.json` を合体させたような立ち位置。
> Spring が起動時に読み込み、アプリ全体に配ってくれる。

## YAML 形式のルール

`.yml` = YAML。**インデント（半角スペース）で階層**を表す。

```yaml
spring:          # 親
  datasource:    #   子（スペース2つ下げ）
    url: ...     #     孫
```

JSON で書くと `{ "spring": { "datasource": { "url": "..." } } }` と同じ。

- インデントは**半角スペース**（タブ禁止）
- `キー: 値` の**コロンの後にスペース必須**

## `${...}` 環境変数の展開

```yaml
url: jdbc:postgresql://localhost:${POSTGRES_PORT:5432}/${POSTGRES_DB:prac}
```

```
${ 環境変数名 : デフォルト値 }
   ↑あれば使う   ↑無ければこれ
```

- 環境変数があればその値、無ければコロンの後のデフォルト値。
- ローカルはデフォルト値で動き、本番は環境変数（AWS Secret Manager 等から注入）で上書きする、
  という運用を1行で両立できる。これが「秘密情報をコードに直書きしない」基本形。

## yml と properties

同じ設定を2つの書き方で書ける。中身は同じ。今は yml が主流。

| 形式 | 例 |
|------|-----|
| yml（本プロジェクト） | `spring:` → `  datasource:` → `    url: xxx` |
| properties | `spring.datasource.url=xxx` |

## プロファイル（環境ごとの設定切り替え）

`application-<プロファイル名>.yml` を用意すると、環境ごとに設定を分けられる。

```
application.yml          ← 共通（全環境で読まれる）
application-local.yml    ← ローカル用
application-prod.yml     ← 本番用
```

### 重要：「共通 + 差分の重ね合わせ」で動く

プロファイルのファイルは共通を**上書き**する形で重なる（全コピーではない）。

```
application.yml（共通）        application-prod.yml（差分）
  app名: prac                    DB URL: 本番サーバー   ← これだけ上書き
  DB URL: localhost      ──►
  Flyway: 有効
        ↓ prod 起動時の最終結果
  app名: prac（共通のまま） / DB URL: 本番（上書き） / Flyway: 有効（共通のまま）
```

→ 共通に書いたものは全環境で効く。プロファイルには**その環境だけの差分**を書く。

### 切り替え方法

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
SPRING_PROFILES_ACTIVE=prod  ./gradlew bootRun
```

> TS の `NODE_ENV` で `.env.development` / `.env.production` を出し分けるのと同じ発想。
> 違いは、Spring が「共通 + 環境差分」を**自動でマージ**してくれる点。

## 実例：local プロファイルで SQL ログを出す

`application-local.yml` に差分だけを書いた例。

```yaml
# local プロファイル用（差分だけ）
spring:
  exposed:
    show-sql: true   # 共通は未指定(=false)。ここで true に上書き
```

`SPRING_PROFILES_ACTIVE=local ./gradlew bootRun` で起動すると、
起動ログに `The following 1 profile is active: "local"` と出て、
実行された SQL がログに出る:

```
SQL: SELECT messages.id, messages."content", messages.created_at FROM messages
SQL: INSERT INTO messages ("content") VALUES ('...')
```

→ INSERT に `id` / `created_at` が含まれないのは `.databaseGenerated()` の効果
（[09-exposed.md](./09-exposed.md) 参照）。DSL が生 SQL に翻訳される様子も確認できる。

## まとめ

- `application.yml` = Spring の設定ファイル（`src/main/resources/`）
- YAML はインデントで階層を表す（タブ禁止）
- `${環境変数:デフォルト}` で秘密情報を外から注入
- **プロファイル**で環境ごとに設定を切り替え（共通 + 差分の重ね合わせ）
- 切り替えは `SPRING_PROFILES_ACTIVE=xxx`
