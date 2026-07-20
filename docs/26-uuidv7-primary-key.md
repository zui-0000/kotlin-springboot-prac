# UUIDv7 を主キーにする（bigserial からの移行）

> `t_message` / `t_user` の主キーを bigint 連番（bigserial）から **UUIDv7** に変えた判断の記録。
> PostgreSQL 18 / Exposed 1.3.1 / 2026-07-20 に移行。

## なぜ連番（bigserial）をやめたか

連番 PK は手軽だが弱点がある。

- **推測可能**: `/messages/1`, `/2`, … と辿れて件数や存在が漏れる（列挙・スクレイピング・IDOR のリスク）。
- **分散・マージに弱い**: 別 DB やシャードで採番すると衝突する。
- **採番に DB シーケンス調整が要る**: 事前に id を決められない（INSERT するまで確定しない）。

UUID なら「グローバル一意・非連番・次を推測しにくい」を得られる。

## なぜ UUIDv4 でなく v7 か

| | v4（ランダム） | v7（時刻順） |
|---|---|---|
| 中身 | 完全ランダム | 先頭がミリ秒タイムスタンプ + ランダム |
| インデックス局所性 | **悪い**（挿入があちこちに飛び B-tree が断片化） | **良い**（追記が末尾に寄る） |
| 参考計測 | — | インデックス約 26–27% 小・順スキャン約 3 倍速 |

- **代償**: v7 は id から**生成時刻が読める**（作成時刻を隠したいリソースには不向き）。また 16 バイト（bigint は 8 バイト）なのでインデックスは連番より大きめ。
- **TS 比較**: `uuid` npm の v4 を PK にすると同じ断片化に当たる。近年は v7 が「ランダム UUID は遅い問題」の定石解。

## PostgreSQL 18 のネイティブ `uuidv7()`

- **PG18（2025-09）で `uuidv7()` が組み込み関数**になった。拡張なしで書ける:
  ```sql
  id uuid NOT NULL DEFAULT uuidv7()
  ```
- おまけで `uuidv4()`（`gen_random_uuid()` の別名）も追加。
- 本プロジェクトでは**マイグレーション適用時に実 DB で動作実証済み**（推測ではなく裏取り）。

## どう配線したか（3層）

| 層 | 書き方 | 意図 |
|---|---|---|
| Flyway（正） | `id uuid NOT NULL DEFAULT uuidv7()` | 採番は DB 側 |
| Exposed | `val id = uuid("id").databaseGenerated()` | INSERT 時に触らせず DB default に任せる（`created_at` と同じ発想） |
| ドメイン | `@JvmInline value class MessageId(val value: UUID)` | 素の UUID を取り違えないよう VO で型付け |

- **注意**: Exposed 1.x の `uuid()` 列は `java.util.UUID` ではなく **`kotlin.uuid.Uuid`** を扱う。
  ドメイン/DTO の正準型は `java.util.UUID` にし、**Exposed アダプタの境界で相互変換**する
  （[09-exposed.md](./09-exposed.md) の「ハマりどころ⑤」参照）。

## OpenAPI 契約側

- `type: string` + `format: uuid` にする → openapi-generator は **`java.util.UUID`** を生成。
  これでドメイン/DTO の正準型（java.util.UUID）と wire が一致する。

## 破壊的移行だったこと

bigint → uuid は**互換キャストが無い**ため、`t_message` は `DROP` して作り直した（ローカルの捨てデータ前提）。
適用済みマイグレーションは不変なので、変更は新規ファイルとして追加する（[11-flyway-migrations.md](./11-flyway-migrations.md)）。

## 関連

- [09-exposed.md](./09-exposed.md) … `uuid()` 列と `kotlin.uuid.Uuid` の境界変換
- [24-db-naming-convention.md](./24-db-naming-convention.md) … テーブル命名（`t_message` / `t_user`）
- [11-flyway-migrations.md](./11-flyway-migrations.md) … マイグレーション運用
