# 現在のスキーマを把握する方法

> マイグレーション方式（Flyway）の弱点＝「今どうなっているか」が一目で見えない、
> への対処法のメモ。

## 背景：マイグレーション方式のトレードオフ

Flyway は変更を1個ずつ積む方式のため、以下を得る代わりに以下を失う。

| 得るもの | 失うもの |
|---------|---------|
| 履歴（どう変わってきたか）・再現性 | **現在の状態の一覧性**（今どうなっているか） |

→ 「今の状態」は**マイグレーションファイル群とは別の場所から**見るのが正解。
　（マイグレーション = レシピ / DB = 完成したケーキ。今の形はケーキを見ればよい）

## 現在の状態を見る4つの方法

### ① DB 本体に聞く（最も確実）
DB 自身が現在状態の正解を持つ。

```bash
# 現在の全スキーマを DDL で出力
docker exec prac-postgres pg_dump -U prac -d prac --schema-only

# テーブル構造を表形式で表示
docker exec prac-postgres psql -U prac -d prac -c "\d t_message"
```

### ② ORM（Exposed）の Table 定義
`Messages.kt` の `Table` 定義がそのまま「現在の構造の宣言」になっている。
Flyway と同期していれば（Exposed 側は合わせる側）、これを読めば今の形が分かる。

### ③ GUI ツール
DataGrip（JetBrains 製）/ DBeaver / TablePlus 等で接続すると、
テーブル・カラム・リレーションを図やツリーで一望できる。実務で最速。

### ④ スキーマのスナップショットをコミットする（本プロジェクトで採用）
「現在の全スキーマ」を1ファイル（`src/main/resources/db/schema.sql`）に生成して Git 管理する。
適用には使わず、**一目で見る & レビューする専用**。
Rails の `schema.rb`、Prisma の `schema.prisma` と同じ発想。

## schema.sql スナップショットの運用

### 生成方法
`schema.sql` は **マイグレーション実行時に自動更新**される。単独の生成コマンドは用意しない。

```bash
mise run db-migrate   # flywayMigrate → dump-schema.sh（schema.sql も更新）
mise run dev          # 上記 db-migrate を実行してからアプリ起動（schema.sql も更新される）
```

- **手で編集しないこと**。自動生成ファイルのため、先頭に警告ヘッダを付けている
  （Rails の `schema.rb` / Prisma の `schema.prisma` と同じ運用）。
- Git では追跡する（PR でスキーマ差分をレビューできるようにするため）。

`dump-schema.sh` は **システムカタログから「1テーブル＝1つの CREATE TABLE 文」を組み立てる**。
`flyway_schema_history`（Flyway の管理テーブル）は除外し、自前のスキーマだけを出力する。

### なぜ pg_dump をそのまま使わないか（MySQL 経験者向け）
- `pg_dump` はテーブル定義を**複数の文に分割**する（`CREATE TABLE` + `CREATE SEQUENCE` +
  `ALTER TABLE ... PRIMARY KEY` 等）。これは PostgreSQL の制限ではなく **pg_dump の出力スタイル**。
- MySQL の `mysqldump` / `SHOW CREATE TABLE` は**1文に集約**するので、その感覚に合わせたい場合は
  pg_dump の出力ではなく、カタログから自前で組み立てる（本スクリプトの方式）。
- 自動採番列（`default` が `nextval(...)`）は `bigserial` 等の表記に戻し、制約は
  インラインで出力するため、MySQL 風の読みやすい1文になる。
- 対応範囲は「列・型・NOT NULL・DEFAULT・**主キー・UNIQUE・外部キー**」。制約は
  `pg_get_constraintdef` で正規の定義文をインライン出力する（PG18 でカタログ化された NOT NULL 制約
  `contype='n'` は、列インラインの `NOT NULL` と二重にならないよう除外する）。索引など更に必要に
  なればスクリプトのクエリを拡張する（それでも足りなければ `pg_dump` で確認する）。

### 位置づけ（重要）
- **`src/main/resources/db/schema.sql` は参照専用**。適用はしない。スキーマの「正」はあくまで Flyway。
- スキーマを変えたら（＝新しいマイグレーションを足したら）、このスクリプトを再実行して
  `src/main/resources/db/schema.sql` を更新し、マイグレーションと一緒にコミットする。
- これにより「変更履歴（migration）」と「現在の姿（schema.sql）」の両方が Git に残る。

## Prisma との対比（TS 経験者向け）

「現在状態を一目で見たい」という要望は、Prisma が最初から両立している。

| | Flyway（生 SQL） | Prisma |
|---|---|---|
| 現在の状態 | 見えにくい → 本ドキュメントの①〜④で補う | `schema.prisma` で一目瞭然 |
| 履歴 | マイグレーションファイル | マイグレーションフォルダ |
| 両立 | 手動で工夫（schema.sql スナップショット等） | 標準で両取り |

## 補足：修正が大変になるわけではない

「ファイルが増えて修正が大変」と感じやすいが、修正自体はむしろ安全:
- 巨大な schema ファイルを書き換えるのではなく、小さい `ALTER` を新ファイルに足すだけ。
- 差分が明確でレビューしやすく、履歴も追える。
- 「大変」なのは *現在状態の把握* だけで、それは①〜④で解決する。

## 関連

- Flyway マイグレーションの仕組み（[11-flyway-migrations.md](./11-flyway-migrations.md)）
- Exposed の Table 定義（[09-exposed.md](./09-exposed.md)）
