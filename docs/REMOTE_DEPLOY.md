# Student Analytics - 远程部署文档

## 概述

`build-and-push.sh` 脚本用于在本地构建项目，然后推送到远程服务器并自动部署。

---

## 使用前准备

### 1. 配置远程服务器信息

复制 `.env.example` 为 `.env`，并配置远程服务器信息：

```bash
cp .env.example .env
```

编辑 `.env` 文件，添加以下配置：

```env
# 远程部署配置
REMOTE_SERVER=192.168.31.238      # 远程服务器 IP 地址
REMOTE_USER=root                  # 登录用户名
REMOTE_PASSWORD=your_password     # 登录密码
REMOTE_PORT=22                    # SSH 端口
REMOTE_DIR=/opt/student-analytics # 远程部署目录
```

### 2. 安装 sshpass（用于密码认证）

**Ubuntu/Debian**:
```bash
sudo apt update
sudo apt install sshpass
```

**macOS**:
```bash
brew install hudochenkov/sshpass/sshpass
```

**CentOS/RHEL**:
```bash
sudo yum install sshpass
```

**Windows (WSL)**:
```bash
sudo apt update
sudo apt install sshpass
```

### 3. 测试连接

```bash
# 测试 SSH 连接
sshpass -p 'your_password' ssh -o StrictHostKeyChecking=no root@192.168.31.238 echo "连接成功"
```

---

## 快速开始

### 一键部署

```bash
chmod +x build-and-push.sh
./build-and-push.sh
```

---

## 脚本执行流程

```
1. 检查环境
   └─ 验证必要命令（mvn、npm、rsync、sshpass、ssh）
   └─ 检查 .env 文件（REMOTE_SERVER、REMOTE_USER、REMOTE_PASSWORD）
   └─ 测试远程服务器连接

2. 构建前端
   └─ npm install
   └─ npm run build
   └─ 生成 frontend/dist/

3. 构建后端
   └─ mvn clean
   └─ mvn package -DskipTests
   └─ 生成 target/*.jar

4. 准备部署文件
   └─ 复制前端 dist 目录
   └─ 复制后端 JAR 文件
   └─ 复制 Docker 相关文件
   └─ 复制数据库脚本

5. 推送到远程服务器
   └─ 使用 sshpass + rsync 同步文件
   └─ 支持增量传输和断点续传

6. 远程部署
   └─ 停止旧服务
   └─ 构建新 Docker 镜像
   └─ 启动新服务
   └─ 清理旧镜像
```

---

## 常用场景

### 场景 1: 全量部署（构建 + 推送 + 部署）

```bash
./build-and-push.sh
```

### 场景 2: 仅推送代码（不构建）

如果已经构建好，可以使用 rsync 直接推送：

```bash
rsync -avz --progress \
  -e "sshpass -p 'your_password' ssh -o StrictHostKeyChecking=no -p 22" \
  --exclude='.git' \
  --exclude='node_modules' \
  --exclude='target' \
  ./ root@192.168.31.238:/opt/student-analytics/

# 然后在远程服务器上执行部署
sshpass -p 'your_password' ssh -o StrictHostKeyChecking=no root@192.168.31.238 \
  'cd /opt/student-analytics && docker compose -f docker-compose.prod.yml up -d --build'
```

### 场景 3: 仅查看远程服务状态

```bash
sshpass -p 'your_password' ssh -o StrictHostKeyChecking=no root@192.168.31.238 \
  'cd /opt/student-analytics && docker compose -f docker-compose.prod.yml ps'
```

### 场景 4: 查看远程日志

```bash
sshpass -p 'your_password' ssh -o StrictHostKeyChecking=no root@192.168.31.238 \
  'cd /opt/student-analytics && docker compose -f docker-compose.prod.yml logs -f app'
```

---

## 远程服务器操作

### 连接到远程服务器

```bash
# 使用 sshpass 密码认证连接
sshpass -p 'your_password' ssh -o StrictHostKeyChecking=no root@192.168.31.238

# 进入项目目录
cd /opt/student-analytics
```

### 查看服务状态

```bash
docker compose -f docker-compose.prod.yml ps
```

### 查看日志

```bash
# 查看所有服务日志
docker compose -f docker-compose.prod.yml logs

# 实时跟踪应用日志
docker compose -f docker-compose.prod.yml logs -f app

# 查看最近 100 行日志
docker compose -f docker-compose.prod.yml logs --tail=100 app
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

---

## 故障排查

### SSH 连接失败

**错误信息**: `无法连接到远程服务器`

**解决方案**:
1. 检查 `.env` 文件配置：
   - `REMOTE_SERVER` 是否正确（IP 地址或域名）
   - `REMOTE_USER` 是否正确
   - `REMOTE_PASSWORD` 是否正确
2. 检查网络连接：
   ```bash
   ping 192.168.31.238
   ```
3. 手动测试连接：
   ```bash
   sshpass -p 'your_password' ssh -o StrictHostKeyChecking=no root@192.168.31.238
   ```
4. 检查 SSH 服务是否运行（在远程服务器上）：
   ```bash
   systemctl status sshd
   ```
5. 检查防火墙是否允许 22 端口（在远程服务器上）：
   ```bash
   sudo ufw status
   # 或
   sudo firewall-cmd --list-all
   ```

### sshpass 命令未找到

**错误信息**: `[错误] 未找到命令: sshpass`

**解决方案**:

Ubuntu/Debian:
```bash
sudo apt update
sudo apt install sshpass
```

macOS:
```bash
brew install hudochenkov/sshpass/sshpass
```

CentOS/RHEL:
```bash
sudo yum install sshpass
```

### 密码包含特殊字符

**问题**: 如果密码包含 `$`、`!`、`'` 等特殊字符，可能会导致 shell 解析错误。

**解决方案**: 在 `.env` 文件中使用单引号包裹密码：

```env
REMOTE_PASSWORD='P@$$w0rd!123'
```

### 前端构建失败

**错误信息**: `npm install` 或 `npm run build` 失败

**解决方案**:
1. 删除 node_modules: `rm -rf frontend/node_modules`
2. 重新安装依赖: `cd frontend && npm install`
3. 检查 Node.js 版本: `node -v`（需要 >= 18）

### 后端构建失败

**错误信息**: `mvn package` 失败

**解决方案**:
1. 检查 Java 版本: `java -version`（需要 17）
2. 检查 Maven 版本: `mvn -v`
3. 清理构建缓存: `mvn clean`

### rsync 传输失败

**错误信息**: `rsync` 失败

**解决方案**:
1. 检查远程服务器磁盘空间：
   ```bash
   sshpass -p 'your_password' ssh -o StrictHostKeyChecking=no root@192.168.31.238 'df -h'
   ```
2. 检查文件权限：
   ```bash
   sshpass -p 'your_password' ssh -o StrictHostKeyChecking=no root@192.168.31.238 'ls -la /opt/student-analytics'
   ```
3. 使用 `--dry-run` 参数测试：
   ```bash
   rsync -avz --dry-run \
     -e "sshpass -p 'your_password' ssh -o StrictHostKeyChecking=no -p 22" \
     ./ root@192.168.31.238:/opt/student-analytics/
   ```

### Docker 部署失败

**错误信息**: `docker compose up` 失败

**解决方案**:
1. 查看详细日志：
   ```bash
   sshpass -p 'your_password' ssh -o StrictHostKeyChecking=no root@192.168.31.238 \
     'cd /opt/student-analytics && docker compose -f docker-compose.prod.yml logs'
   ```
2. 检查 Docker 状态：
   ```bash
   sshpass -p 'your_password' ssh -o StrictHostKeyChecking=no root@192.168.31.238 'docker ps -a'
   ```
3. 检查 .env 配置是否正确
4. 重新构建镜像：
   ```bash
   sshpass -p 'your_password' ssh -o StrictHostKeyChecking=no root@192.168.31.238 \
     'cd /opt/student-analytics && docker compose -f docker-compose.prod.yml build --no-cache'
   ```

---

## 环境变量配置

确保远程服务器上存在 `.env` 文件，并配置以下参数：

```env
# 端口配置
MYSQL_PORT=3307
MONGO_PORT=27017
APP_PORT=8090

# 数据库密码
MYSQL_ROOT_PASSWORD=your_secure_password
DB_PASSWORD=your_secure_password

# OpenAI API Key（可选）
OPENAI_API_KEY=your_openai_api_key
OPENAI_BASE_URL=https://api.openai.com
```

---

## 安全建议

1. **修改默认密码**: 不要使用 `root123`，使用强密码
2. **保护 .env 文件**:
   - 不要将 `.env` 提交到版本控制系统
   - 确保 `.env` 文件权限为 600：
     ```bash
     chmod 600 .env
     ```
   - 在 `.gitignore` 中添加 `.env`
3. **使用环境变量**: 脚本使用 `.env` 文件配置敏感信息，避免在脚本中硬编码
4. **限制数据库端口访问**: 在远程服务器上配置防火墙，只允许内部访问 3307、27017
5. **定期更新**: 保持 Docker 镜像和依赖库更新
6. **使用 HTTPS**: 生产环境建议配置反向代理（如 Nginx）并启用 HTTPS
7. **限制 SSH 访问**: 在远程服务器上配置 `sshd_config`，限制允许登录的用户和 IP

**关于密码认证的说明**：
- 密码认证适合内网环境或测试环境
- 生产环境建议使用 SSH 密钥认证（更安全）
- 如果使用密码认证，请确保：
  - 密码足够复杂
  - 服务器已配置 fail2ban 等安全工具
  - 定期更换密码

---

## 性能优化

### 1. 增量传输

rsync 默认只传输变化的文件，大幅提升传输速度。

### 2. 并行构建

如果前端和后端可以独立构建，可以考虑使用并行构建：

```bash
# 前端构建（后台）
(cd frontend && npm run build) &

# 后端构建（后台）
(mvn package -DskipTests) &

# 等待所有构建完成
wait
```

### 3. Docker 多阶段构建

Dockerfile 使用多阶段构建，最终镜像只包含运行时依赖，减小镜像体积。

---

## 验证部署

部署完成后，验证服务是否正常：

```bash
# 1. 检查健康状态
curl http://192.168.31.238:8090/actuator/health

# 2. 访问应用
curl http://192.168.31.238:8090/

# 3. 查看日志
ssh user@192.168.31.238 'docker compose -f docker-compose.prod.yml logs app'
```

---

## 回滚

如果部署出现问题，可以快速回滚到上一个版本：

```bash
# 连接到远程服务器
sshpass -p 'your_password' ssh -o StrictHostKeyChecking=no root@192.168.31.238

# 停止当前服务
cd /opt/student-analytics
docker compose -f docker-compose.prod.yml down

# 切换到上一个版本（假设有 Git 记录）
git log --oneline -5
git checkout <commit-hash>

# 重新部署
docker compose -f docker-compose.prod.yml up -d --build
```

或者使用 Docker 镜像回滚：

```bash
# 查看可用镜像
sshpass -p 'your_password' ssh -o StrictHostKeyChecking=no root@192.168.31.238 'docker images | grep student-analytics'

# 使用旧镜像启动
sshpass -p 'your_password' ssh -o StrictHostKeyChecking=no root@192.168.31.238 \
  'cd /opt/student-analytics && docker compose -f docker-compose.prod.yml up -d --no-deps app'
```

---

## 附录

### 与 deploy.sh 的区别

| 脚本 | 使用场景 | 执行位置 |
|------|----------|----------|
| `deploy.sh` | 在演示服务器上直接部署 | 远程服务器 |
| `build-and-push.sh` | 本地构建，推送到远程部署 | 本地机器 |

- **deploy.sh**: 适用于已经在服务器上拉取代码，直接在服务器上构建和部署
- **build-and-push.sh**: 适用于在本地构建（可能因为本地资源充足），然后推送到服务器部署

### 相关脚本

- `deploy.sh` - 演示服务器一键部署
- `start.bat` - 本地开发一键启动（Windows）
- `stop.bat` - 本地开发一键停止（Windows）
- `build-and-push.sh` - 远程部署脚本（本脚本）