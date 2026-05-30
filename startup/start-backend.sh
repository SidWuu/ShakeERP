#!/bin/bash
# 一键重启后端

cd "$(dirname "$0")/../backend"

# 切换 Java 17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"

# 停掉已有的后端进程
lsof -ti :8080 | xargs kill -9 2>/dev/null

# 确保 data 和 uploads 目录存在
mkdir -p ../data ../uploads

echo "启动后端 (Java $(java -version 2>&1 | head -1 | awk -F'"' '{print $2}'))..."
mvn spring-boot:run
