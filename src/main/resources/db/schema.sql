-- ⚠️ このファイルは自動生成です。直接編集しないでください。
-- 更新: マイグレーション時（mise run db-migrate / mise run dev）に自動再生成されます。

CREATE TABLE messages (
    id bigserial NOT NULL,
    content text NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    PRIMARY KEY (id)
);
