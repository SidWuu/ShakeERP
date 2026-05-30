#!/bin/bash
# 一键重启前后端

DIR="$(dirname "$0")/.."

# 停掉已有进程
lsof -ti :5173 | xargs kill -9 2>/dev/null
lsof -ti :8080 | xargs kill -9 2>/dev/null

sleep 1

# 启动后端（后台运行）
echo "启动后端..."
cd "$DIR/backend"
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"
mkdir -p ../data ../uploads
mvn spring-boot:run &
BACKEND_PID=$!

# 等后端启动
sleep 5

# 启动前端（前台运行）
echo "启动前端..."
cd "$DIR/frontend"
npm run dev -- --host 127.0.0.1 &
FRONTEND_PID=$!

echo ""
echo "================================"
echo "  后端: http://localhost:8080"
echo "  前端: http://127.0.0.1:5173"
echo "================================"
echo ""
echo "按 Ctrl+C 停止所有服务"

# 捕获退出信号，停止子进程
trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit" INT TERM
wait
