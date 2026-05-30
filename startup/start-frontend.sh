#!/bin/bash
# 一键重启前端

cd "$(dirname "$0")/../frontend"

# 停掉已有的前端进程
lsof -ti :5173 | xargs kill -9 2>/dev/null

echo "启动前端..."
npm run dev -- --host 127.0.0.1
