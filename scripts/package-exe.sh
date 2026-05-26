#!/bin/bash
# ConfigGen 打包脚本
#
# 用法:
#   ./package-exe.sh          # 打包 JAR
#   ./package-exe.sh exe     # 打包 Windows exe (需要 jpackage)
#
# 注意: jpackage 需要 JDK 17+ 并配置了 JAVA_HOME

set -e

echo "========================================="
echo "ConfigGen 打包脚本"
echo "========================================="

# 检查 Maven
if ! command -v mvn &> /dev/null; then
    echo "错误: Maven 未安装"
    echo "请安装 Maven: https://maven.apache.org/install.html"
    exit 1
fi

# 打包 JAR
echo "正在构建 JAR..."
mvn clean package -DskipTests -q

if [ "$1" = "exe" ]; then
    # 检查 jpackage
    if ! command -v jpackage &> /dev/null; then
        echo "警告: jpackage 未安装，跳过 exe 打包"
        echo "JAR 已生成: target/config-gen.jar"
        exit 0
    fi

    echo "正在打包 Windows exe..."
    jpackage \
        --type app-image \
        --input target/ \
        --dest dist/ \
        --name ConfigGen \
        --main-jar config-gen.jar \
        --vendor "ConfigGen" \
        --app-version "1.0.0" \
        --java-options "-Xmx512m"

    echo "exe 已生成: dist/ConfigGen/"
else
    echo "JAR 已生成: target/config-gen.jar"
fi

echo "========================================="
echo "打包完成!"
echo "========================================="