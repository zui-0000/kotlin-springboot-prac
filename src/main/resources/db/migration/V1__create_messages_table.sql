-- 最初のマイグレーション: messages テーブルを作成
CREATE TABLE messages (
    id         BIGSERIAL PRIMARY KEY,
    content    TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
