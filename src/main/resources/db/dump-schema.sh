#!/usr/bin/env bash
#
# 現在の DB スキーマを このディレクトリの schema.sql に出力する。
# 「1テーブル＝1つの CREATE TABLE 文」に集約した、一目で読める参照専用ファイル。
# ★適用には使わない（スキーマの正はあくまで Flyway のマイグレーション）。
#
# 使い方: DB を起動した状態で実行（どのディレクトリからでも可）
#   docker compose up -d
#   ./src/main/resources/db/dump-schema.sh
#
set -euo pipefail

# スクリプト自身の位置を基準にパスを解決（CWD に依存しない）
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# src/main/resources/db から 4 つ上がプロジェクトルート
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
OUT="$SCRIPT_DIR/schema.sql"

# .env があれば読み込む（POSTGRES_USER 等）
if [ -f "$PROJECT_ROOT/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  . "$PROJECT_ROOT/.env"
  set +a
fi

CONTAINER="prac-postgres"
DB_USER="${POSTGRES_USER:-prac}"
DB_NAME="${POSTGRES_DB:-prac}"

# システムカタログから「1テーブル＝1文」の CREATE TABLE を組み立てる。
# pg_dump は定義を複数文に分割する（CREATE SEQUENCE / ALTER ...）ため、
# MySQL の mysqldump のように1文へ集約したい場合は、この自前クエリ方式を使う。
#   - 自動採番（default が nextval(...) の列）は bigserial/serial 表記に戻す
#   - PRIMARY KEY はインラインで出力
#   - flyway_schema_history（Flyway 管理テーブル）は除外
# 先頭に「編集禁止」の警告ヘッダを付けてから、生成した CREATE TABLE を書き出す。
# psql オプション: -t 値のみ / -A 整形なし / -X psqlrc無視 / -q 静音
{
  echo "-- ⚠️ このファイルは自動生成です。直接編集しないでください。"
  echo "-- 更新: マイグレーション時（mise run db-migrate / mise run dev）に自動再生成されます。"
  echo ""
  docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -X -q <<'SQL'
SELECT string_agg(table_ddl, E'\n\n' ORDER BY tablename)
FROM (
  SELECT
    c.relname AS tablename,
    'CREATE TABLE ' || quote_ident(c.relname) || E' (\n'
    || (
      SELECT string_agg(
        '    ' || quote_ident(a.attname) || ' ' || x.col_type || x.col_notnull || x.col_default,
        E',\n' ORDER BY a.attnum
      )
      FROM pg_attribute a
      LEFT JOIN pg_attrdef ad ON ad.adrelid = a.attrelid AND ad.adnum = a.attnum
      CROSS JOIN LATERAL (
        SELECT
          CASE
            WHEN pg_get_expr(ad.adbin, ad.adrelid) LIKE 'nextval(%' THEN
              CASE format_type(a.atttypid, a.atttypmod)
                WHEN 'bigint'   THEN 'bigserial'
                WHEN 'integer'  THEN 'serial'
                WHEN 'smallint' THEN 'smallserial'
                ELSE format_type(a.atttypid, a.atttypmod)
              END
            ELSE format_type(a.atttypid, a.atttypmod)
          END AS col_type,
          CASE WHEN a.attnotnull THEN ' NOT NULL' ELSE '' END AS col_notnull,
          CASE
            WHEN ad.adbin IS NOT NULL AND pg_get_expr(ad.adbin, ad.adrelid) NOT LIKE 'nextval(%'
              THEN ' DEFAULT ' || pg_get_expr(ad.adbin, ad.adrelid)
            ELSE ''
          END AS col_default
      ) x
      WHERE a.attrelid = c.oid AND a.attnum > 0 AND NOT a.attisdropped
    )
    || COALESCE(
      (SELECT E',\n    PRIMARY KEY (' || string_agg(quote_ident(att.attname), ', ' ORDER BY k.ord) || ')'
       FROM pg_constraint con
       CROSS JOIN LATERAL unnest(con.conkey) WITH ORDINALITY AS k(attnum, ord)
       JOIN pg_attribute att ON att.attrelid = con.conrelid AND att.attnum = k.attnum
       WHERE con.conrelid = c.oid AND con.contype = 'p'
       GROUP BY con.oid),
      ''
    )
    || E'\n);' AS table_ddl
  FROM pg_class c
  JOIN pg_namespace n ON n.oid = c.relnamespace
  WHERE n.nspname = 'public' AND c.relkind = 'r'
    AND c.relname <> 'flyway_schema_history'
) t;
SQL
} > "$OUT"

echo "現在のスキーマを $OUT に出力しました"
