#!/bin/bash
# ============================================================
# Student Analytics - 构建并推送到远程服务器
# 用途：本地构建后端和前端，然后推送到远程服务器部署
# 认证方式：密码认证（从 .env 文件读取）
# ============================================================

set -e

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# ============================================================
# 加载配置
# ============================================================
if [ ! -f .env ]; then
    echo -e "${RED}[错误] 未找到 .env 文件${NC}"
    echo -e "${YELLOW}[提示] 请创建 .env 文件并配置以下内容：${NC}"
    echo ""
    cat <<'EOF'
# 远程服务器配置
REMOTE_SERVER=192.168.31.238
REMOTE_USER=root
REMOTE_PASSWORD=your_password
REMOTE_PORT=22
REMOTE_DIR=/opt/student-analytics
EOF
    exit 1
fi

# 加载 .env 文件（忽略注释）
export $(grep -v '^#' .env | grep -v '^$' | xargs)

# 验证必要配置
if [ -z "$REMOTE_SERVER" ] || [ -z "$REMOTE_USER" ] || [ -z "$REMOTE_PASSWORD" ]; then
    echo -e "${RED}[错误] .env 文件缺少必要的配置${NC}"
    echo -e "${YELLOW}[提示] 请配置以下变量：REMOTE_SERVER, REMOTE_USER, REMOTE_PASSWORD${NC}"
    exit 1
fi

# 设置默认值
REMOTE_PORT=${REMOTE_PORT:-22}
REMOTE_DIR=${REMOTE_DIR:-/opt/student-analytics}
SSH_CMD="sshpass -p '${REMOTE_PASSWORD}' ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -p ${REMOTE_PORT} ${REMOTE_USER}@${REMOTE_SERVER}"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Student Analytics - 构建并推送${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "  服务器: ${REMOTE_USER}@${REMOTE_SERVER}:${REMOTE_PORT}"
echo -e "  目录: ${REMOTE_DIR}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 检查必要的命令
echo -e "${YELLOW}[检查] 验证必要命令...${NC}"
for cmd in mvn npm rsync sshpass ssh; do
    if ! command -v $cmd &> /dev/null; then
        echo -e "${RED}[错误] 未找到命令: $cmd${NC}"
        if [ "$cmd" = "sshpass" ]; then
            echo -e "${YELLOW}[提示] 安装 sshpass: Ubuntu/Debian: apt install sshpass | macOS: brew install hudochenkov/sshpass/sshpass${NC}"
        fi
        exit 1
    fi
done
echo -e "${GREEN}[成功] 所有必要命令可用${NC}"
echo ""

# 测试 SSH 连接
echo -e "${YELLOW}[检查] 测试远程服务器连接...${NC}"
if ! sshpass -p "${REMOTE_PASSWORD}" ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5 -p ${REMOTE_PORT} ${REMOTE_USER}@${REMOTE_SERVER} echo "SSH连接成功" 2>/dev/null; then
    echo -e "${RED}[错误] 无法连接到远程服务器${NC}"
    echo -e "${YELLOW}[提示] 请检查: 1) 服务器地址 2) 用户名 3) 密码 4) 网络连接${NC}"
    exit 1
fi
echo -e "${GREEN}[成功] 远程服务器连接正常${NC}"
echo ""

# --------------------------------------------------------
# 1. 构建前端
# --------------------------------------------------------
echo -e "${YELLOW}[1/5] 构建前端...${NC}"
cd frontend
echo -e "${YELLOW}[  ] 安装依赖...${NC}"
npm install
echo -e "${YELLOW}[  ] 构建生产版本...${NC}"
npm run build
cd ..
echo -e "${GREEN}[成功] 前端构建完成${NC}"
echo ""

# --------------------------------------------------------
# 2. 构建后端
# --------------------------------------------------------
echo -e "${YELLOW}[2/5] 构建后端...${NC}"
echo -e "${YELLOW}[  ] 清理旧构建...${NC}"
mvn clean
echo -e "${YELLOW}[  ] 打包应用...${NC}"
mvn package -DskipTests
echo -e "${GREEN}[成功] 后端构建完成${NC}"
echo ""

# --------------------------------------------------------
# 3. 准备部署文件
# --------------------------------------------------------
echo -e "${YELLOW}[3/5] 准备部署文件...${NC}"

# 创建临时部署目录
DEPLOY_DIR="./deploy-tmp"
rm -rf $DEPLOY_DIR
mkdir -p $DEPLOY_DIR

# 复制必要文件
cp -r frontend/dist $DEPLOY_DIR/ 2>/dev/null || echo -e "${YELLOW}[警告] 未找到前端构建产物${NC}"
cp target/*.jar $DEPLOY_DIR/app.jar 2>/dev/null || echo -e "${YELLOW}[警告] 未找到后端 JAR 文件${NC}"
cp docker-compose.prod.yml $DEPLOY_DIR/ 2>/dev/null || echo -e "${YELLOW}[警告] 未找到 docker-compose.prod.yml${NC}"
cp Dockerfile $DEPLOY_DIR/ 2>/dev/null || echo -e "${YELLOW}[警告] 未找到 Dockerfile${NC}"
cp .env $DEPLOY_DIR/ 2>/dev/null || echo -e "${YELLOW}[警告] 未找到 .env 文件${NC}"
cp -r sql $DEPLOY_DIR/ 2>/dev/null || echo -e "${YELLOW}[警告] 未找到 sql 目录${NC}"

# 检查关键文件
if [ ! -f $DEPLOY_DIR/app.jar ]; then
    echo -e "${RED}[错误] 未找到 JAR 文件${NC}"
    echo -e "${YELLOW}[提示] 请检查后端构建是否成功${NC}"
    exit 1
fi

echo -e "${GREEN}[成功] 部署文件准备完成${NC}"
echo ""

# --------------------------------------------------------
# 4. 推送到远程服务器
# --------------------------------------------------------
echo -e "${YELLOW}[4/5] 推送到远程服务器...${NC}"

# 创建远程目录
echo -e "${YELLOW}[  ] 创建远程目录...${NC}"
sshpass -p "${REMOTE_PASSWORD}" ssh -o StrictHostKeyChecking=no -p ${REMOTE_PORT} ${REMOTE_USER}@${REMOTE_SERVER} "mkdir -p ${REMOTE_DIR}"

# 同步文件（使用 rsync，支持增量传输和断点续传）
echo -e "${YELLOW}[  ] 同步文件到远程服务器...${NC}"
rsync -avz --progress \
    -e "sshpass -p '${REMOTE_PASSWORD}' ssh -o StrictHostKeyChecking=no -p ${REMOTE_PORT}" \
    --exclude='.git' \
    --exclude='node_modules' \
    --exclude='target' \
    --exclude='.DS_Store' \
    --exclude='deploy-tmp' \
    ./ ${REMOTE_USER}@${REMOTE_SERVER}:${REMOTE_DIR}/

echo -e "${GREEN}[成功] 文件已推送到远程服务器${NC}"
echo ""

# --------------------------------------------------------
# 5. 在远程服务器上部署
# --------------------------------------------------------
echo -e "${YELLOW}[5/5] 在远程服务器上部署...${NC}"

sshpass -p "${REMOTE_PASSWORD}" ssh -o StrictHostKeyChecking=no -p ${REMOTE_PORT} ${REMOTE_USER}@${REMOTE_SERVER} << ENDSSH
cd ${REMOTE_DIR}

# 停止旧服务
echo "[  ] 停止旧服务..."
docker compose -f docker-compose.prod.yml down || true

# 拉取最新镜像（如果使用多阶段构建）
echo "[  ] 构建新镜像..."
docker compose -f docker-compose.prod.yml build --no-cache

# 启动新服务
echo "[  ] 启动新服务..."
docker compose -f docker-compose.prod.yml up -d

# 清理未使用的镜像
echo "[  ] 清理旧镜像..."
docker image prune -f

echo "[成功] 远程部署完成"

# 显示服务状态
echo ""
echo "========================================"
echo "  服务状态"
echo "========================================"
docker compose -f docker-compose.prod.yml ps
ENDSSH

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  构建并推送完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "  服务器: ${REMOTE_USER}@${REMOTE_SERVER}:${REMOTE_PORT}"
echo -e "  目录: ${REMOTE_DIR}"
echo -e "  访问地址: http://${REMOTE_SERVER}:8090"
echo -e "${GREEN}========================================${NC}"
echo ""

# 清理临时文件
rm -rf $DEPLOY_DIR
echo -e "${GREEN}[提示] 临时文件已清理${NC}"