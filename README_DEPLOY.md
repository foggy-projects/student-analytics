# Student Analytics - 演示服务器部署文档

## 快速开始

### 一键部署（推荐）

在演示服务器上执行：

```bash
# 1. 克隆代码（如果尚未克隆）
git clone https://github.com/foggy-projects/student-analytics.git
cd student-analytics

# 2. 执行部署脚本
chmod +x deploy.sh
./deploy.sh
```

---

## 详细步骤

### 1. 环境要求

- **Docker**: >= 20.10
- **Docker Compose**: >= 2.0
- **Git**

### 2. 配置环境变量

首次部署前，创建 `.env` 配置文件：

```bash
cp .env.example .env
```

编辑 `.env` 文件，修改以下配置：

```env
# 端口配置
MYSQL_PORT=3307
MONGO_PORT=27017
APP_PORT=8090

# 数据库密码
MYSQL_ROOT_PASSWORD=your_secure_password
DB_PASSWORD=your_secure_password

# OpenAI API Key（可选，用于 AI 功能）
OPENAI_API_KEY=your_openai_api_key
```

### 3. 构建并启动服务

```bash
# 方式一：使用部署脚本
./deploy.sh

# 方式二：手动执行
docker compose -f docker-compose.prod.yml up -d --build
```

### 4. 验证服务状态

```bash
# 查看所有服务状态
docker compose -f docker-compose.prod.yml ps

# 检查应用健康状态
curl http://localhost:8090/actuator/health
```

---

## 服务说明

| 服务 | 端口 | 说明 | 健康检查 |
|------|------|------|---------|
| MySQL | 3307 | 主数据库 | 自动初始化 schema.sql |
| MongoDB | 27017 | MongoDB 数据库 | 无需初始化 |
| App | 8090 | Spring Boot 应用 | /actuator/health |

---

## 常用命令

### 查看日志

```bash
# 查看所有服务日志
docker compose -f docker-compose.prod.yml logs

# 查看指定服务日志
docker compose -f docker-compose.prod.yml logs app
docker compose -f docker-compose.prod.yml logs mysql
```

### 重启服务

```bash
# 重启所有服务
docker compose -f docker-compose.prod.yml restart

# 重启单个服务
docker compose -f docker-compose.prod.yml restart app
```

### 停止服务

```bash
# 停止所有服务
docker compose -f docker-compose.prod.yml down

# 停止并删除数据卷（⚠️ 会删除所有数据）
docker compose -f docker-compose.prod.yml down -v
```

### 进入容器

```bash
# 进入应用容器
docker exec -it student-analytics-app sh

# 进入 MySQL 容器
docker exec -it student-analytics-mysql mysql -uroot -p

# 进入 MongoDB 容器
docker exec -it student-analytics-mongo mongosh
```

---

## 数据库初始化

MySQL 数据库会在首次启动时自动执行 `sql/schema.sql` 初始化表结构。

如需手动执行初始化：

```bash
docker exec -i student-analytics-mysql mysql -uroot -pstudent_analytics < sql/schema.sql
```

---

## 故障排查

### 应用无法连接数据库

检查服务启动顺序和健康状态：

```bash
docker compose -f docker-compose.prod.yml ps
```

确保 MySQL 和 MongoDB 均显示 `healthy` 状态。

### 端口冲突

修改 `.env` 文件中的端口配置，然后重启：

```bash
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml up -d
```

### 重新构建镜像

```bash
docker compose -f docker-compose.prod.yml build --no-cache
docker compose -f docker-compose.prod.yml up -d
```

---

## 开发环境 vs 生产环境

| 文件 | 用途 |
|------|------|
| `docker/docker-compose.yml` | 本地开发环境（仅数据库，应用用 Maven 启动） |
| `docker-compose.prod.yml` | 演示/生产环境（全 Docker 化） |

本地开发使用 `start.bat` 启动；
演示服务器使用 `./deploy.sh` 部署。

---

## 更新部署

当有新代码推送时：

```bash
# 拉取最新代码
git pull origin master

# 重新构建并启动
docker compose -f docker-compose.prod.yml up -d --build
```

---

## 安全建议

- 修改默认密码（`MYSQL_ROOT_PASSWORD`、`DB_PASSWORD`）
- 不要将 `.env` 文件提交到版本控制
- 在生产环境中配置防火墙规则，限制外部访问数据库端口（3307、27017）
- 使用 HTTPS（需配置 Nginx 反向代理）