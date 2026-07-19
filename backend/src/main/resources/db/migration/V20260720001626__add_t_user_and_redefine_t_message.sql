-- ユーザーテーブル(t_user)を新設し、t_message を UUIDv7 主キー + user_id(FK) + updated_at 付きへ再定義する。
--
-- 背景:
--  - 主キーを bigint 連番 → UUIDv7(uuid) へ変更。PostgreSQL 18 ネイティブの uuidv7() を DEFAULT に使う
--    （時刻順プレフィックスを持つため v4 と違い B-tree 局所性が良い。拡張不要）。
--  - t_message.user_id は t_user(id) への外部キー(NOT NULL)。投稿の所有者を表す。
--  - updated_at を追加（INSERT は DEFAULT now()、UPDATE 時はアプリが now() をセットする方針）。
--
-- ※ bigint → uuid は互換キャストが無いため、t_message は DROP して作り直す（破壊的・ローカルの捨てデータ前提）。
--   適用済みマイグレーションは不変なので、変更は必ず新規ファイルとして追加する（docs/11）。

CREATE TABLE t_user (
    id         uuid        NOT NULL DEFAULT uuidv7(),
    name       text        NOT NULL,
    email      text        NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_t_user PRIMARY KEY (id),
    CONSTRAINT uq_t_user_email UNIQUE (email)
);

DROP TABLE t_message;

CREATE TABLE t_message (
    id         uuid        NOT NULL DEFAULT uuidv7(),
    user_id    uuid        NOT NULL,
    content    text        NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_t_message PRIMARY KEY (id),
    CONSTRAINT fk_t_message_user FOREIGN KEY (user_id) REFERENCES t_user (id)
);
