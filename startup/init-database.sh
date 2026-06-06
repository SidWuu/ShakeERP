#!/bin/bash
# 上线前初始化数据库：建表、基础字典、初始用户

set -euo pipefail

DIR="$(cd "$(dirname "$0")/.." && pwd)"
DB_PATH="${1:-$DIR/data/factory-erp.db}"
INIT_SQL="$DIR/backend/src/main/resources/db/init.sql"

if ! command -v sqlite3 >/dev/null 2>&1; then
  echo "未找到 sqlite3，请先安装 sqlite3 后再执行初始化。"
  exit 1
fi

if [ ! -f "$INIT_SQL" ]; then
  echo "初始化 SQL 不存在：$INIT_SQL"
  exit 1
fi

mkdir -p "$(dirname "$DB_PATH")"

sqlite3 "$DB_PATH" < "$INIT_SQL"

echo "数据库初始化完成：$DB_PATH"
