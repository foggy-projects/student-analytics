# Student Analytics - Claude Memory

> AI 驱动的学生数据分析系统（单校部署）

## 项目结构
- `src/` - Spring Boot 后端（Java 17 + Spring Boot 3.4.5）
- `frontend/` - Vue 3 前端（Vite + TypeScript）
- `sql/` - 数据库 DDL
- `docs/design/` - 设计文档

## 技术栈
- **后端**: Spring Boot 3.4.5 + foggy-dataset-model 8.1.4.beta
- **前端**: Vue 3 + foggy-data-viewer
- **数据库**: MySQL
- **包名**: com.foggysource.student

## 数据模型
- 一期 MVP：16 张表（详见 docs/design/data-model.md）
- 表前缀：`dim_`（维度）、`fact_`（事实）、`agg_`（聚合）
- PK 统一 BIGINT AUTO_INCREMENT
- 排名通过 QM 窗口函数实时计算，不存储
- TM/QM 模型文件位置：`src/main/resources/foggy/templates/student/model/`

## API 规范
- 使用 RX 统一返回（继承 foggy-core 规范）
- 端口：8090

## 开发约定
- 构建跳过测试：`-DskipTests`
- 前端开发：`cd frontend && npm run dev`
