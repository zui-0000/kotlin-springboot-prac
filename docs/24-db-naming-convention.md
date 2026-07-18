# DB テーブルの命名規則（プレフィックス）

> テーブル名は **種別プレフィックス + 単数形** で付ける。`messages` → `t_message` のように。
> 実例は `message` 機能（Flyway: `V*__rename_messages_to_t_message.sql` / Exposed: `TMessage`）。

## 規則

| 種別 | プレフィックス | 意味 | 例 |
|---|---|---|---|
| **トランザクション系** | `t_` | 業務で**積み上がっていく**データ（投稿・注文・ログ等） | `t_message` / `t_order` |
| **マスタ系** | `m_` | **参照・定義**のための静的データ（区分・カテゴリ・ユーザー種別等） | `m_user` / `m_category` |

- **形は単数形**（`t_messages` ではなく `t_message`）。
- 日本のエンプラ開発で定番の規約。**テーブル名の先頭を見た瞬間に種別が分かる**のが利点。

### t_ と m_ の見分け方

> 「そのデータは**イベントとして増え続けるか**、**あらかじめ定義される台帳か**」で判断する。

- **増え続ける** → トランザクション（`t_`）。例: メッセージ投稿、注文、アクセスログ
- **あらかじめ用意する台帳** → マスタ（`m_`）。例: ユーザー区分、商品カテゴリ、都道府県

`messages` はユーザー投稿が積み上がるので **トランザクション系 → `t_message`**。

## Kotlin(Exposed)側の命名

Exposed のテーブルオブジェクトは **DB 名に寄せて `TMessage`** とする（`Table("t_message")`）。

```kotlin
object TMessage : Table("t_message") {
    val id = long("id").autoIncrement()
    // ...
}
```

- **なぜ単数の `Message` にしないか**：ドメインの集約 `Message`（`domain/`）と名前が衝突するため。
  `TMessage` なら衝突せず、DB 名との対応も一目で分かる。
- オブジェクト名（Kotlin 識別子）と DB 名の対応は、`Table("...")` の**文字列引数**で決まる。
  つまり Kotlin 側の名前は DB 名と独立して選べるが、本プロジェクトでは**寄せる方針**とする。

## 変更・リネームの手順（重要）

テーブル名を変える／新設するときは、**Flyway の鉄則**に従う。

- **適用済みマイグレーションは絶対に編集しない**。変更は**新規ファイル**で行う。
- リネームは `ALTER TABLE 旧名 RENAME TO 新名;` を新しいマイグレーションに書く。

```sql
-- V<YYYYMMDDHHMMSS>__rename_messages_to_t_message.sql
ALTER TABLE messages RENAME TO t_message;
```

- 適用は `mise run db-migrate`（`flywayMigrate` → `schema.sql` 再生成）。
- `schema.sql` は自動生成物。手で編集しない（詳細は [11-flyway-migrations.md](./11-flyway-migrations.md) / [12-current-schema-visibility.md](./12-current-schema-visibility.md)）。

## 新しいテーブルを作るときのチェックリスト

1. **種別を判定** … 積み上がる=`t_` / 台帳=`m_`
2. **単数形**で命名 … `t_xxx` / `m_xxx`
3. 新規マイグレーションで `CREATE TABLE`（最初から規則に沿った名前で作る）
4. Exposed オブジェクトは `TXxx` / `MXxx`（集約名との衝突を避けつつ DB 名に対応）
5. `mise run db-migrate` で適用 & `schema.sql` 更新

## まとめ

- **`t_`=トランザクション / `m_`=マスタ**、**単数形**
- Exposed オブジェクトは **DB 名に寄せて `TMessage`**（集約 `Message` と衝突回避）
- リネーム・新設は**新規マイグレーション**で（適用済みは編集しない）
