#!/bin/bash
# 一键启动前后端（HTTPS 模式，用于手机扫码）

DIR="$(dirname "$0")/.."

export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"

# 停掉旧进程
lsof -ti :8443 | xargs kill -9 2>/dev/null
lsof -ti :5173 | xargs kill -9 2>/dev/null
sleep 1

# 检查/生成证书
if [ ! -f "$DIR/backend/src/main/resources/keystore.p12" ]; then
    echo "证书不存在，正在生成..."
    cd "$DIR/backend/src/main/resources" && ./generate-cert.sh && cd "$DIR"
fi

mkdir -p "$DIR/data" "$DIR/uploads"

# 启动 HTTPS 后端（后台）
echo "启动 HTTPS 后端（端口 8443）..."
cd "$DIR/backend"
mvn spring-boot:run -Dspring-boot.run.profiles=https &
BACKEND_PID=$!

sleep 6

# 启动前端（指向 HTTPS 后端）
echo "启动前端..."
cd "$DIR/frontend"
VITE_API_BASE_URL=https://localhost:8443 npm run dev -- --host 0.0.0.0 &
FRONTEND_PID=$!

sleep 2

echo ""
echo "================================"
echo "  后端(HTTPS): https://localhost:8443"
echo "  前端: http://localhost:5173"
echo ""
echo "  手机扫码访问后端: https://电脑IP:8443"
echo "  手机访问前端: http://电脑IP:5173"
echo "  (首次需在手机浏览器信任自签证书)"
echo "================================"
echo ""
echo "按 Ctrl+C 停止所有服务"

trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit" INT TERM
wait
