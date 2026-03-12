# MCP 数据录入服务设计报告

## 1. 概述

| 项目 | 说明 |
|------|------|
| 服务端点 | `/mcp/input/rpc` |
| 所属应用 | student-analytics（同一 Spring Boot 应用，不同路径） |
| 查询服务 | `/mcp/analyst/rpc`（已有，只读） |
| 目标 | 通过 MCP 协议实现自然语言数据录入 |

## 2. 数据录入分类

根据表的性质，将录入操作分为三类：

| 类别 | 表 | 录入频率 | 说明 |
|------|-----|---------|------|
| **高频录入** | fact_score, fact_attendance | 每天/每次考试 | 核心业务数据 |
| **低频录入** | dim_student, dim_exam, dim_class | 学期初/考试前 | 维度基础数据 |
| **系统生成** | agg_student_profile, fact_learning_advice, dim_date | 自动 | 不开放手动录入 |

## 3. MCP Tools 设计

### 3.1 score.create — 录入成绩

**描述**：录入单条学生成绩。通过学号精确定位学生，写入 fact_score 表。

**典型对话**：
- 「录入学号 070101 这次期中考试数学 95 分」
- 「于嘉俊（070103）期末考试语文 112 分」

**参数**：

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| student_no | string | ✅ | 学号（唯一标识） |
| subject_name | string | ✅ | 科目名称（语文/数学/英语/物理/化学/道德与法治/历史/地理/生物） |
| exam_name | string | ✅ | 考试名称（如「2025-2026学年第一学期期中考试」） |
| score | number | ✅ | 分数（DECIMAL 5,2） |
| score_level | string | ❌ | 成绩等级（A/B/C/D），不传则自动计算 |

**后端处理逻辑**：
1. 通过 student_no 查 dim_student → 获取 student_id、class_id、student_name
2. 通过 subject_name 查 dim_subject → 获取 subject_id，同时校验 score ≤ full_score
3. 通过 exam_name 查 dim_exam → 获取 exam_id
4. 若 score_level 未传，自动计算：score ≥ excellent_score → A，≥ pass_score → B，≥ pass_score×0.8 → C，否则 D
5. 检查是否已存在相同 (student_id, subject_id, exam_id) 的记录 → 存在则提示用 score.update
6. 返回确认信息：「已录入：于嘉俊(070103) | 期末考试 | 语文 | 112分 | A」

**错误处理**：
- student_no 不存在 → 提示「学号 070199 未找到，请检查或先用 student.create 创建」
- exam_name 模糊匹配 → 返回最相近的候选
- subject_name 不存在 → 返回已有科目列表

---

### 3.2 score.update — 修改成绩

**描述**：修改已有的学生成绩记录。

**典型对话**：
- 「把 070101 期中考试的数学成绩改成 98 分」
- 「修改于嘉俊（070103）上学期期末语文成绩为 115」

**参数**：

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| student_no | string | ✅ | 学号 |
| subject_name | string | ✅ | 科目名称 |
| score | number | ✅ | 新分数 |
| exam_name | string | ❌ | 考试名称，不传时按策略自动定位 |
| score_level | string | ❌ | 新等级，不传则自动计算 |

**后端处理逻辑**：
1. 通过 student_no 查 dim_student → 获取 student_id
2. 查找匹配的 fact_score 记录
3. exam_name 处理策略：
   - **已指定** → 精确匹配该考试
   - **未指定 + 仅 1 条记录** → 自动选中
   - **未指定 + 多条记录** → 返回候选列表，包含考试名称和原始分数，让 AI 引导用户选择
4. 返回变更预览：「于嘉俊(070103) | 期末考试 | 数学 | 92 → 98 | B → A」
5. 执行更新

---

### 3.3 score.batch_create — 批量录入成绩

**描述**：批量录入一次考试的多条成绩，适用于一个班级/一个科目的场景。Skill 识别试卷图片后的主要调用入口。

**典型对话**：
- 「录入初三(1)班这次期中考试的数学成绩：070101 95分、070102 87分、070103 92分」
- Skill 识别试卷图片后调用此接口（试卷上有学号）

**参数**：

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| exam_name | string | ✅ | 考试名称 |
| subject_name | string | ✅ | 科目名称 |
| scores | array | ✅ | 成绩数组 |
| scores[].student_no | string | ✅ | 学号 |
| scores[].score | number | ✅ | 分数 |
| scores[].score_level | string | ❌ | 等级，不传自动计算 |

**后端处理逻辑**：
1. 批量通过 student_no 解析 student_id、class_id
2. 逐条校验（学号是否存在、分数是否超满分、是否重复录入）
3. 返回汇总预览：「本次共 38 条，其中新增 35 条，已存在 3 条（跳过），学号异常 0 条」
4. 批量 INSERT，失败的单独列出（含学号和原因）

---

### 3.4 exam.create — 创建考试

**描述**：新建一场考试记录，录入成绩前需要先有考试。

**典型对话**：
- 「创建一场月考，初三第三次月考，日期 3 月 15 号」
- 「新建考试：2025-2026学年第一学期随堂测验三，日期2026-01-05」

**参数**：

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| exam_name | string | ✅ | 考试名称 |
| exam_type | string | ✅ | 考试类型（daily/unit/midterm/final） |
| exam_date | string | ✅ | 考试日期（YYYY-MM-DD） |
| semester_name | string | ❌ | 所属学期名称，不传则根据 exam_date 自动匹配 is_current 学期 |

**后端处理逻辑**：
1. 检查 exam_name 是否已存在 → 存在则提示
2. semester_name 处理：
   - 已指定 → 查 dim_semester 匹配
   - 未指定 → 根据 exam_date 落在哪个学期的 start_date~end_date 区间自动匹配
   - 都匹配不到 → 取 is_current=1 的学期
3. 返回确认：「已创建考试：2025-2026学年第一学期第三次月考 | 月考 | 2026-03-15 | 当前学期」

---

### 3.5 student.create — 新增学生

**描述**：新增一名学生到指定班级。

**典型对话**：
- 「初一(1)班新转来一个学生李华，男，2012年5月出生」
- 「添加学生：王小明，初二(3)班，女」

**参数**：

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| student_name | string | ✅ | 学生姓名 |
| class_name | string | ✅ | 班级名称（如「初一(1)班」） |
| gender | string | ❌ | 性别（M/F），不传则不填 |
| birth_date | string | ❌ | 出生日期（YYYY-MM-DD） |
| student_no | string | ❌ | 学号，不传则自动生成 |
| phone | string | ❌ | 联系电话 |

**后端处理逻辑**：
1. 通过 class_name 查 dim_class → 获取 class_id（模糊匹配，如「初一1班」「七年级一班」都能匹配）
2. 检查同班级是否已有同名学生 → 有则提示
3. student_no 自动生成规则：年级编号 + 班级序号 + 自增序号（如 070138）
4. 插入后自动更新 dim_class.student_count
5. 返回：「已添加：李华 | 初一(1)班 | 男 | 学号 070139」

---

### 3.6 attendance.record — 记录考勤

**描述**：记录学生考勤状态。

**典型对话**：
- 「今天 090101 和 090102 请假了」
- 「记录 070105 今天上午迟到」

**参数**：

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| student_nos | array\<string\> | ✅ | 学号列表（支持多人） |
| date | string | ❌ | 日期（YYYY-MM-DD），默认今天 |
| status | string | ✅ | 状态（absent/late/leave_early/sick_leave） |
| time_slot | string | ❌ | 时段（morning/afternoon/evening） |
| reason | string | ❌ | 原因说明 |

**后端处理逻辑**：
1. 批量通过 student_nos 解析 → 获取每人的 student_id、class_id、student_name
2. date 转为 date_id 格式（YYYY-MM-DD），校验 dim_date 中是否存在
3. 检查是否已有相同 (student_id, date_id, time_slot) 记录 → 有则提示更新
4. 批量插入
5. 返回：「已记录 2 人考勤：张三(090101) 请假、李四(090102) 请假 | 2026-03-12」

---

## 4. 批次追踪与回撤

### 4.1 为什么需要批次

老师批量录入成绩时可能出错（录错科目、录错考试、张冠李戴），需要支持**整批回撤**。

### 4.2 批次表设计

新增 `sys_batch_log` 表：

```sql
CREATE TABLE sys_batch_log (
    batch_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_type  VARCHAR(30)  NOT NULL COMMENT '操作类型: score_create/score_update/attendance_record/student_create/exam_create',
    target_table VARCHAR(50) NOT NULL COMMENT '目标表: fact_score/fact_attendance/dim_student/dim_exam',
    record_count INT         NOT NULL COMMENT '影响记录数',
    status      VARCHAR(20)  NOT NULL DEFAULT 'committed' COMMENT 'committed/rolled_back',
    operator    VARCHAR(50)  NULL     COMMENT '操作来源标识（如 MCP session）',
    summary     VARCHAR(500) NULL     COMMENT '操作摘要，如「期中考试 | 数学 | 38条成绩」',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    rolled_back_at TIMESTAMP NULL
) COMMENT '数据录入批次日志';
```

### 4.3 工作机制

**写入时**：所有写入操作（含单条）自动分配 batch_id

- fact_score 表增加 `batch_id BIGINT NULL` 列
- fact_attendance 表增加 `batch_id BIGINT NULL` 列
- dim_student 表增加 `batch_id BIGINT NULL` 列
- dim_exam 表增加 `batch_id BIGINT NULL` 列

**返回时**：每次写入操作的响应中包含 batch_id

```json
{
  "status": "success",
  "batch_id": 42,
  "summary": "期中考试 | 数学 | 录入38条成绩",
  "message": "如需撤销，使用 batch.rollback 并提供批次号 42"
}
```

### 4.4 batch.rollback — 按批次回撤（新增 Tool）

**描述**：按批次号回撤一次录入操作。

**典型对话**：
- 「刚才录错了，撤销批次 42」
- 「把上一次批量录入的成绩撤回」

**参数**：

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| batch_id | number | ✅ | 批次号 |

**后端处理逻辑**：
1. 查 sys_batch_log 确认批次存在且 status = 'committed'
2. 根据 batch_type 决定回撤方式：
   - **score_create** → DELETE FROM fact_score WHERE batch_id = ?
   - **score_update** → 需要 snapshot，见 4.5
   - **student_create** → DELETE FROM dim_student WHERE batch_id = ?，同时回退 class.student_count
   - **exam_create** → DELETE FROM dim_exam WHERE batch_id = ?（需检查是否已关联成绩）
   - **attendance_record** → DELETE FROM fact_attendance WHERE batch_id = ?
3. 更新 sys_batch_log: status = 'rolled_back', rolled_back_at = NOW()
4. 返回：「批次 42 已回撤：删除了 38 条成绩记录（期中考试 | 数学）」

**安全约束**：
- 已回撤的批次不可重复回撤
- exam.create 回撤时，若该考试已有关联成绩 → 拒绝回撤，提示先撤成绩

### 4.5 score.update 的快照机制

修改成绩需要记录原始值才能回撤，新增 `sys_update_snapshot` 表：

```sql
CREATE TABLE sys_update_snapshot (
    snapshot_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id     BIGINT NOT NULL,
    target_table VARCHAR(50) NOT NULL,
    record_id    BIGINT NOT NULL COMMENT '被修改记录的主键',
    old_values   JSON   NOT NULL COMMENT '修改前的字段值',
    new_values   JSON   NOT NULL COMMENT '修改后的字段值',
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_batch (batch_id)
) COMMENT '数据修改快照（用于回撤）';
```

score.update 回撤时：读取 old_values，UPDATE 还原。

### 4.6 batch.list — 查询批次历史（新增 Tool）

**描述**：查询最近的操作批次，方便找到要回撤的批次号。

**典型对话**：
- 「最近录入了哪些数据？」
- 「查看最近的操作记录」

**参数**：

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| limit | number | ❌ | 返回条数，默认 10 |
| status | string | ❌ | 过滤状态（committed/rolled_back），默认 committed |

**返回示例**：

```
批次 42 | 2026-03-12 14:30 | 录入成绩 | 期中考试 | 数学 | 38条 | ✅ 有效
批次 41 | 2026-03-12 14:25 | 录入成绩 | 期中考试 | 语文 | 38条 | ✅ 有效
批次 40 | 2026-03-12 10:00 | 记录考勤 | 3人请假 | ✅ 有效
批次 39 | 2026-03-11 16:00 | 录入成绩 | 月考 | 英语 | 38条 | ❌ 已回撤
```

---

## 5. 工具总览

| Tool | 操作 | 目标表 | 写入类型 |
|------|------|--------|---------|
| `score.create` | 录入单条成绩 | fact_score | INSERT |
| `score.update` | 修改成绩 | fact_score | UPDATE + SNAPSHOT |
| `score.batch_create` | 批量录入成绩 | fact_score | BATCH INSERT |
| `exam.create` | 创建考试 | dim_exam | INSERT |
| `student.create` | 新增学生 | dim_student | INSERT |
| `attendance.record` | 记录考勤 | fact_attendance | BATCH INSERT |
| `batch.rollback` | 按批次回撤 | 根据 batch_type | DELETE / RESTORE |
| `batch.list` | 查询批次历史 | sys_batch_log | SELECT |

> 共 8 个 Tools：6 个写入 + 1 个回撤 + 1 个查询

## 6. 通用设计原则

### 6.1 职责边界：MCP 只做精准写入

```
┌─────────────────────────────────────────────────────────┐
│  OpenClaw / AI 客户端（脏活累活）                        │
│                                                         │
│  · 用户说姓名 → 调查询MCP查学号 → 用学号调录入MCP        │
│  · 试卷图片 → AI 视觉识别姓名 → 匹配学号                │
│  · 手写模糊 → 根据班级、成绩习惯推测学生身份              │
│  · 数据校验、确认提示、歧义消解                          │
└──────────────────────┬──────────────────────────────────┘
                       │ 精确的 student_no + 结构化参数
                       ▼
┌─────────────────────────────────────────────────────────┐
│  MCP 录入服务 /mcp/input/rpc（精准写入）                 │
│                                                         │
│  · 接收学号（唯一标识），不做模糊匹配                     │
│  · 校验数据完整性（学号存在、分数不超满分、不重复录入）     │
│  · 分配批次号，记录操作日志                               │
│  · 写入数据库，返回结果 + batch_id                       │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│  MySQL 数据库                                           │
│  fact_score / fact_attendance / dim_* + sys_batch_log    │
└─────────────────────────────────────────────────────────┘
```

**核心原则：MCP 录入服务不猜、不推断、不模糊匹配。它只做三件事：校验、写入、记录批次。**

### 6.2 标识解析策略

| 字段 | 解析方 | 匹配策略 |
|------|--------|---------|
| **student_no** | MCP 录入服务 | **精确匹配**（唯一键，不存在即报错） |
| subject_name | MCP 录入服务 | 精确匹配 |
| exam_name | MCP 录入服务 | 精确匹配 + 模糊回退 |
| class_name | MCP 录入服务 | 模糊匹配（「初一1班」→「初一(1)班」） |
| semester_name | MCP 录入服务 | 模糊匹配 + 日期推断 |
| student_name → student_no | **OpenClaw 负责** | AI 通过查询 MCP 解析 |

### 6.3 错误响应规范

```json
{
  "status": "not_found",
  "message": "学号 070199 未找到",
  "suggestion": "请检查学号，或使用 student.create 创建新学生"
}
```

### 6.4 成绩等级自动计算

基于 dim_subject 的分数线配置：

| 条件 | 等级 |
|------|------|
| score ≥ excellent_score | A |
| score ≥ pass_score | B |
| score ≥ pass_score × 0.8 | C |
| score < pass_score × 0.8 | D |

例：语文（满分120，及格72，优秀102）→ 112分 = A，85分 = B，60分 = C，50分 = D

### 6.5 级联更新

| 操作 | 级联 |
|------|------|
| student.create | 自动更新 dim_class.student_count +1 |
| score.create | 若首次录入该学生成绩，自动创建 agg_student_profile 空记录 |
| batch.rollback (student) | 自动回退 dim_class.student_count -N |

## 7. 与 Skill（试卷图片录入）的衔接

```
用户：「把 ~/exams/math-midterm/ 下的试卷都录进去」
         │
         ▼
  ┌─ Skill 扫描目录，读取每张图片
  │
  │  AI 视觉识别：学号 + 各题分数 → 总分
  │  （如果试卷上只有姓名没有学号，AI 根据班级花名册匹配学号）
  │  （模糊场景：根据笔迹、成绩分布、座位号等辅助推断）
  │
  │  Skill 展示识别结果，用户确认
  └─────────────┬───────────────
                │ 确认后
                ▼
  调用 MCP score.batch_create
  {
    exam_name: "2025-2026学年第一学期期中考试",
    subject_name: "数学",
    scores: [
      { student_no: "070103", score: 99.1 },
      { student_no: "090101", score: 95.0 },
      ...
    ]
  }
                │
                ▼
  MCP 返回：{ batch_id: 42, success: 38, message: "..." }
                │
                ▼
  Skill 展示结果：「已录入 38 条，批次号 42（如需撤销可告诉我）」
```

**职责分离**：
- **Skill** → 图片识别、学号匹配、用户确认（AI 的活）
- **MCP** → 精准写入、批次记录、支持回撤（数据库的活）
