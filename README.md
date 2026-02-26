# Student Analytics - AI 驱动的学生数据分析系统

> AI 驱动的学生数据分析系统（单校部署）

---

## 项目简介

本系统为中小学教育场景提供一套 AI 驱动的数据分析解决方案，核心目标：

- 🎯 **协助老师完成对学生的个性化教育**
- 👀 **让老师和家长及时掌握学生的教育情况**
- ⚠️ **及时发现问题，并采用合适的方案**

### 核心特性

- **AI 自然语言查询**：支持用自然语言查询学生数据、成绩分析
- **多维度分析**：成绩、考勤、学生画像全方位覆盖
- **个性化建议**：基于数据自动生成学习建议
- **预警机制**：及时发现成绩、考勤异常情况

---

## 技术栈

| 层级 | 技术 |
|------|------|
| **后端** | Spring Boot 3.4.5 + Java 17 |
| **前端** | Vue 3 + Vite + TypeScript |
| **数据库** | MySQL 8.0 + MongoDB 7 |
| **AI 引擎** | Foggy Dataset Model (TM/QM 语义模型) |

---

## 快速开始

### 本地开发

```bash
# Windows 一键启动
start.bat

# Linux/Mac 手动启动
docker compose -f docker/docker-compose.yml up -d
mvn spring-boot:run -DskipTests
```

### 演示服务器部署

详细部署文档请查看：[README_DEPLOY.md](./README_DEPLOY.md)

```bash
# 一键部署
chmod +x deploy.sh
./deploy.sh
```

---

## 项目结构

```
student-analytics/
├── src/                          # Spring Boot 后端
│   └── main/
│       ├── java/com/foggysource/student/
│       └── resources/
│           ├── application.yml   # 应用配置
│           └── foggy/templates/  # TM/QM 语义模型
│               ├── student/model/    # 16 个 TM 模型
│               └── student/query/    # 16 个 QM 查询模型
├── frontend/                     # Vue 3 前端
├── sql/                          # 数据库 DDL
│   └── schema.sql                # 16 张表的完整 DDL
├── docker/                       # Docker 配置
│   └── docker-compose.yml        # 本地开发环境
├── docker-compose.prod.yml       # 演示/生产环境
├── Dockerfile                    # 后端应用镜像
├── start.bat                     # 本地一键启动
├── deploy.sh                     # 演示服务器一键部署
└── docs/design/                  # 设计文档
    └── data-model.md             # 数据模型设计文档
```

---

## 数据模型

一期 MVP 设计了 **16 张表**，覆盖 5 大业务域：

| 域 | 表数量 | 说明 |
|---|---|---|
| 权限组织 | 3 张 | 用户、教师、任课关系 |
| 时间 | 2 张 | 学期、日期维度 |
| 学生班级 | 3 张 | 年级、班级、学生 |
| 科目考试 | 4 张 | 科目、考试、知识点、闭包 |
| 成绩分析 | 4 张 | 成绩、画像、学习建议、考勤 |

详细设计请查看：[docs/design/data-model.md](./docs/design/data-model.md)

---

## 语义模型

项目使用 Foggy TM/QM 语义模型支撑 AI 自然语言查询：

- **TM 模型（表模型）**：16 个，定义数据表结构和维度关系
- **QM 模型（查询模型）**：16 个，定义可查询的视图和计算字段

### 自然语言查询示例

```
❓ 查71班的成绩
❓ 张三是哪个班的？
❓ 三年级1班的数学成绩排名
```

---

## 开发进度

| 模块 | 完成度 |
|------|--------|
| 设计文档 | 100% ✅ |
| 数据库 DDL | 100% ✅ |
| TM 语义模型 | 100% ✅ (16/16) |
| QM 查询模型 | 100% ✅ (16/16) |
| 后端 Java 代码 | 5% ⏳ (仅脚手架) |
| 前端代码 | 3% ⏳ (仅脚手架) |

**总体进度：约 35%**

---

## 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| 应用 | 8090 | Spring Boot 后端 |
| MySQL | 3307 | 主数据库 |
| MongoDB | 27017 | MongoDB 数据库 |

---

## 文档

- [数据模型设计文档](./docs/design/data-model.md) - 完整的 ER 图、表结构、权限设计
- [部署文档](./README_DEPLOY.md) - 演示服务器部署指南
- [开发日记](./docs/notes/2026-02-20-progress.md) - 项目开发进度记录

---

## 开发约定

- **构建跳过测试**：`mvn clean package -DskipTests`
- **前端开发**：`cd frontend && npm run dev`
- **数据库初始化**：`sql/schema.sql` 挂载到 docker-entrypoint-initdb.d

---

## License

MIT License