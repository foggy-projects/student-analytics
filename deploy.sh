#!/bin/bash
# ============================================================
# Student Analytics - 演示服务器一键部署脚本
# 用途：在演示服务器上构建并启动所有服务
# ============================================================

set -e

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Student Analytics - 部署中...${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 检查 .env 文件
if [ ! -f .env ]; then
    echo -e "${YELLOW}[提示] 首次部署，创建环境配置文件...${NC}"
    cp .env.example .env
    echo -e "${GREEN}[成功] 已创建 .env 配置文件${NC}"
    echo ""
fi

# 拉取最新代码
echo -e "${YELLOW}[1/4] 拉取最新代码...${NC}"
git pull origin master || echo -e "${YELLOW}[警告] Git 拉取失败，继续本地构建${NC}"

# 构建并启动服务
echo -e "${YELLOW}[2/4] 构建并启动所有服务...${NC}"
docker compose -f docker-compose.prod.yml up -d --build

# 等待服务就绪
echo -e "${YELLOW}[3/4] 等待服务启动...${NC}"
sleep 15

# 检查服务健康状态
echo -e "${YELLOW}[4/4] 检查服务状态...${NC}"
docker compose -f docker-compose.prod.yml ps

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  部署完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "  MySQL:  localhost:${MYSQL_PORT:-3307}"
echo -e "  MongoDB:  localhost:${MONGO_PORT:-27017}"
echo -e "  应用:    http://localhost:${APP_PORT:-8090}"
echo -e "  健康检查: http://localhost:${APP_PORT:-8090}/actuator/health"
echo -e "${GREEN}========================================${NC}"
echo ""