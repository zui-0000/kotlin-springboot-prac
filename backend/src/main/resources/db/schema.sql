-- ⚠️ このファイルは自動生成です。直接編集しないでください。
-- 更新: マイグレーション時（mise run db-migrate / mise run dev）に自動再生成されます。

CREATE TABLE t_message (
    id uuid NOT NULL DEFAULT uuidv7(),
    user_id uuid NOT NULL,
    content text NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES t_user(id)
);

CREATE TABLE t_user (
    id uuid NOT NULL DEFAULT uuidv7(),
    name text NOT NULL,
    email text NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    PRIMARY KEY (id),
    UNIQUE (email)
);
