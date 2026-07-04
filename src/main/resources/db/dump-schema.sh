#!/usr/bin/env bash
#
# 現在の DB スキーマを このディレクトリの schema.sql にダンプする。
# これは「今どうなっているか」を一目で見る & レビューするための参照専用ファイル。
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

# --schema-only  : データではなく構造(DDL)だけ
# --no-owner/--no-privileges : 環境依存の所有者・権限情報を省く
# --exclude-table=flyway_schema_history : Flyway の管理テーブルは対象外（自前のスキーマだけ見たい）
docker exec "$CONTAINER" pg_dump -U "$DB_USER" -d "$DB_NAME" \
  --schema-only --no-owner --no-privileges \
  --exclude-table=flyway_schema_history \
  | grep -vE '^\\(un)?restrict ' \
  > "$OUT"

echo "現在のスキーマを $OUT に出力しました"
