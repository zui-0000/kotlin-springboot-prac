-- テーブル命名規則の導入に伴うリネーム。
-- 規則: トランザクション系=t_ / マスタ系=m_（いずれも単数形）。
-- messages（ユーザー投稿が積み上がるトランザクション系）を t_message にリネームする。
ALTER TABLE messages RENAME TO t_message;
