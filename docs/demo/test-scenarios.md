# Student Analytics - 常见查询场景测试

> 基于 DSL 查询语法，覆盖 4 个 QM 模型的典型业务场景
> 服务地址：`http://localhost:8090`
> 查询端点：`POST /api/semantic-layer/query`
>
> 每个场景包含 **老师自然语言提问**（用于 MCP + LLM 测试）和 **期望 DSL**（用于结果校验）

---

## 目录

- [场景 1：查看某次考试的班级成绩单](#场景-1查看某次考试的班级成绩单)
- [场景 2：找出不及格的学生](#场景-2找出不及格的学生)
- [场景 3：各科目平均分对比](#场景-3各科目平均分对比)
- [场景 4：班级内排名（窗口函数）](#场景-4班级内排名窗口函数)
- [场景 5：某个学生的历次考试成绩趋势](#场景-5某个学生的历次考试成绩趋势)
- [场景 6：期中 vs 期末成绩对比](#场景-6期中-vs-期末成绩对比)
- [场景 7：本月考勤异常学生](#场景-7本月考勤异常学生)
- [场景 8：各班出勤率统计](#场景-8各班出勤率统计)
- [场景 9：查看学生画像](#场景-9查看学生画像)
- [场景 10：查看某学生的学习建议](#场景-10查看某学生的学习建议)

---

## 场景 1：查看某次考试的班级成绩单

**业务场景**：班主任想查看"七(1)班"最近一次期中考试的所有学生成绩

> 💬 **老师提问**
>
> - "帮我看一下七(1)班期中考试的成绩"
> - "我想看看我们班这次期中考得怎么样"
> - "把七年级1班期中考试的成绩单拉出来，按科目排一下"

```bash
curl -s -X POST http://localhost:8090/api/semantic-layer/query \
  -H "Content-Type: application/json" \
  -d '{
    "queryModel": "FactScoreQueryModel",
    "columns": [
      "student$caption",
      "student$studentNo",
      "subject$caption",
      "exam$caption",
      "score",
      "scoreLevel"
    ],
    "slice": [
      { "field": "clazz$caption", "op": "=", "value": "七(1)班" },
      { "field": "exam$examType", "op": "=", "value": "midterm" }
    ],
    "orderBy": [
      { "field": "subject$caption", "dir": "asc" },
      { "field": "score", "dir": "desc" }
    ],
    "returnTotal": true
  }' | python -m json.tool
```

**预期结果**：返回七(1)班所有学生的期中考试成绩，按科目分组、分数从高到低排列。

---

## 场景 2：找出不及格的学生

**业务场景**：教务主任想找出本学期所有科目不及格（score < 60）的学生名单

> 💬 **老师提问**
>
> - "有哪些学生不及格？帮我列一下"
> - "把所有科目低于60分的学生找出来"
> - "哪些孩子挂科了，我要看名单和具体分数"
> - "不及格的同学有多少，分别是谁"

```bash
curl -s -X POST http://localhost:8090/api/semantic-layer/query \
  -H "Content-Type: application/json" \
  -d '{
    "queryModel": "FactScoreQueryModel",
    "columns": [
      "student$caption",
      "student$studentNo",
      "student$gender",
      "clazz$caption",
      "subject$caption",
      "exam$caption",
      "exam$examDate",
      "score",
      "scoreLevel"
    ],
    "slice": [
      { "field": "score", "op": "<", "value": 60 }
    ],
    "orderBy": [
      { "field": "score", "dir": "asc" }
    ],
    "returnTotal": true
  }' | python -m json.tool
```

**预期结果**：返回所有成绩低于60分的记录，分数最低的排最前面。`total` 字段可以直观看到不及格总人次。

---

## 场景 3：各科目平均分对比

**业务场景**：教研组长想看各科目在最近一次期末考试中的平均分

> 💬 **老师提问**
>
> - "期末考试各科的平均分是多少"
> - "帮我统计下期末考试每门课的均分，从高到低排"
> - "这次期末哪科考得最好，哪科最差"

```bash
curl -s -X POST http://localhost:8090/api/semantic-layer/query \
  -H "Content-Type: application/json" \
  -d '{
    "queryModel": "FactScoreQueryModel",
    "columns": [
      "subject$caption",
      "exam$caption",
      "score"
    ],
    "slice": [
      { "field": "exam$examType", "op": "=", "value": "final" }
    ],
    "groupBy": [
      { "field": "subject$caption" },
      { "field": "exam$caption" }
    ],
    "orderBy": [
      { "field": "score", "dir": "desc" }
    ],
    "returnTotal": true
  }' | python -m json.tool
```

**预期结果**：每行一个科目，score 列显示该科的平均分（TM 中 score 的聚合方式为 `avg`）。`totalData` 中的 score 为全科目总均分。

---

## 场景 4：班级内排名（窗口函数）

**业务场景**：查看某次考试中，某科目各学生在班级内的排名

> 💬 **老师提问**
>
> - "七(1)班这次期中数学考试的排名情况"
> - "帮我排一下我们班数学期中的名次"
> - "期中考试数学成绩排名，我要看七(1)班的"
> - （变体）"数学期中考试全年级排名前二十是谁"

> 排名通过 `calculatedFields` 的窗口函数实时计算，不存储在数据库中

```bash
curl -s -X POST http://localhost:8090/api/semantic-layer/query \
  -H "Content-Type: application/json" \
  -d '{
    "queryModel": "FactScoreQueryModel",
    "columns": [
      "student$caption",
      "student$studentNo",
      "clazz$caption",
      "score",
      "rankInClass"
    ],
    "slice": [
      { "field": "exam$caption", "op": "like", "value": "期中" },
      { "field": "subject$caption", "op": "=", "value": "数学" },
      { "field": "clazz$caption", "op": "=", "value": "七(1)班" }
    ],
    "calculatedFields": [
      {
        "name": "rankInClass",
        "caption": "班级排名",
        "expression": "RANK() OVER (PARTITION BY class_id, exam_id, subject_id ORDER BY score DESC)"
      }
    ],
    "orderBy": [
      { "field": "rankInClass", "dir": "asc" }
    ]
  }' | python -m json.tool
```

**预期结果**：返回七(1)班数学期中考试成绩，附带班级排名。第1名分数最高。

**变体 — 年级排名**：

```bash
curl -s -X POST http://localhost:8090/api/semantic-layer/query \
  -H "Content-Type: application/json" \
  -d '{
    "queryModel": "FactScoreQueryModel",
    "columns": [
      "student$caption",
      "clazz$caption",
      "score",
      "rankInGrade"
    ],
    "slice": [
      { "field": "exam$caption", "op": "like", "value": "期中" },
      { "field": "subject$caption", "op": "=", "value": "数学" }
    ],
    "calculatedFields": [
      {
        "name": "rankInGrade",
        "caption": "年级排名",
        "expression": "RANK() OVER (PARTITION BY exam_id, subject_id ORDER BY score DESC)"
      }
    ],
    "orderBy": [
      { "field": "rankInGrade", "dir": "asc" }
    ],
    "start": 0,
    "limit": 20
  }' | python -m json.tool
```

**预期结果**：全年级数学期中排名 Top 20。

---

## 场景 5：某个学生的历次考试成绩趋势

**业务场景**：老师想查看张浩然同学各科历次考试成绩变化

> 💬 **老师提问**
>
> - "张浩然这几次考试的成绩怎么样，帮我看看趋势"
> - "20240101这个学生的历次考试成绩给我拉一下"
> - "帮我看看张浩然从入学到现在各科的分数变化"
> - "我想了解一下这个孩子的成绩是在进步还是退步"

```bash
curl -s -X POST http://localhost:8090/api/semantic-layer/query \
  -H "Content-Type: application/json" \
  -d '{
    "queryModel": "FactScoreQueryModel",
    "columns": [
      "exam$caption",
      "exam$examType",
      "exam$examDate",
      "subject$caption",
      "score",
      "scoreLevel"
    ],
    "slice": [
      { "field": "student$studentNo", "op": "=", "value": "20240101" }
    ],
    "orderBy": [
      { "field": "exam$examDate", "dir": "asc" },
      { "field": "subject$caption", "dir": "asc" }
    ]
  }' | python -m json.tool
```

**预期结果**：按时间顺序展示张浩然同学（学号 20240101）所有考试成绩，可直观看到各科分数是上升还是下降。

> 适合做折线图的数据源

---

## 场景 6：期中 vs 期末成绩对比

**业务场景**：对比某班期中和期末的各科平均分变化

> 💬 **老师提问**
>
> - "八(1)班期中和期末的成绩对比一下"
> - "帮我看看八年级1班从期中到期末各科平均分有没有进步"
> - "对比一下我们班期中期末的均分变化"

```bash
curl -s -X POST http://localhost:8090/api/semantic-layer/query \
  -H "Content-Type: application/json" \
  -d '{
    "queryModel": "FactScoreQueryModel",
    "columns": [
      "exam$examType",
      "subject$caption",
      "score"
    ],
    "slice": [
      { "field": "clazz$caption", "op": "=", "value": "八(1)班" },
      { "field": "exam$examType", "op": "in", "value": ["midterm", "final"] }
    ],
    "groupBy": [
      { "field": "exam$examType" },
      { "field": "subject$caption" }
    ],
    "orderBy": [
      { "field": "subject$caption", "dir": "asc" },
      { "field": "exam$examType", "dir": "asc" }
    ]
  }' | python -m json.tool
```

**预期结果**：每个科目出现两行（midterm / final），可直接对比平均分差异。

---

## 场景 7：本月考勤异常学生

**业务场景**：德育处想查看 2025年12月 所有缺勤、迟到、早退的学生

> 💬 **老师提问**
>
> - "12月份有哪些学生迟到或缺勤了"
> - "帮我查一下上个月的考勤异常记录"
> - "2025年12月哪些孩子请假、迟到或者早退了，给我看看明细"
> - "最近一个月考勤有问题的学生都有谁"

```bash
curl -s -X POST http://localhost:8090/api/semantic-layer/query \
  -H "Content-Type: application/json" \
  -d '{
    "queryModel": "FactAttendanceQueryModel",
    "columns": [
      "date$dateValue",
      "date$weekDay",
      "student$caption",
      "student$studentNo",
      "clazz$caption",
      "status",
      "timeSlot",
      "reason"
    ],
    "slice": [
      { "field": "date$year", "op": "=", "value": 2025 },
      { "field": "date$month", "op": "=", "value": 12 },
      { "field": "status", "op": "in", "value": ["absent", "late", "leave_early", "sick_leave"] }
    ],
    "orderBy": [
      { "field": "date$dateValue", "dir": "desc" },
      { "field": "clazz$caption", "dir": "asc" }
    ],
    "returnTotal": true
  }' | python -m json.tool
```

**预期结果**：返回该月所有考勤异常记录。`total` 显示异常总人次。

---

## 场景 8：各班出勤率统计

**业务场景**：统计各班本月的考勤次数（用于计算出勤率）

> 💬 **老师提问**
>
> - "各个班12月份的出勤情况怎么样"
> - "统计一下各班这个月的出勤率"
> - "帮我看看哪个班迟到的最多"

```bash
curl -s -X POST http://localhost:8090/api/semantic-layer/query \
  -H "Content-Type: application/json" \
  -d '{
    "queryModel": "FactAttendanceQueryModel",
    "columns": [
      "clazz$caption",
      "status",
      "attendCount"
    ],
    "slice": [
      { "field": "date$year", "op": "=", "value": 2025 },
      { "field": "date$month", "op": "=", "value": 12 },
      { "field": "date$isSchoolDay", "op": "=", "value": true }
    ],
    "groupBy": [
      { "field": "clazz$caption" },
      { "field": "status" }
    ],
    "calculatedFields": [
      {
        "name": "attendCount",
        "caption": "人次",
        "expression": "attendance_id",
        "agg": "COUNT"
      }
    ],
    "orderBy": [
      { "field": "clazz$caption", "dir": "asc" }
    ]
  }' | python -m json.tool
```

**预期结果**：每班每种出勤状态一行，例如 `七(1)班 | present | 760`，`七(1)班 | late | 12`。

---

## 场景 9：查看学生画像

**业务场景**：班主任想了解班上学生的综合画像（等级 + 趋势 + AI 摘要）

> 💬 **老师提问**
>
> - "帮我看看哪些学生目前是中等和待提升的"
> - "有没有综合评估比较差的学生，我要重点关注"
> - （变体）"最近成绩下降的学生有哪些"
> - （变体）"哪些孩子成绩在退步，帮我筛出来"

```bash
curl -s -X POST http://localhost:8090/api/semantic-layer/query \
  -H "Content-Type: application/json" \
  -d '{
    "queryModel": "AggStudentProfileQueryModel",
    "columns": [
      "student$caption",
      "student$studentNo",
      "student$gender",
      "student$studentStatus",
      "overallLevel",
      "scoreTrend",
      "aiSummary",
      "refreshedAt"
    ],
    "slice": [
      { "field": "overallLevel", "op": "in", "value": ["C", "D"] }
    ],
    "orderBy": [
      { "field": "overallLevel", "dir": "desc" },
      { "field": "refreshedAt", "dir": "desc" }
    ],
    "returnTotal": true
  }' | python -m json.tool
```

**预期结果**：返回综合等级为 C（中等）和 D（待提升）的学生画像，便于重点关注。

**变体 — 成绩下降的学生**：

```bash
curl -s -X POST http://localhost:8090/api/semantic-layer/query \
  -H "Content-Type: application/json" \
  -d '{
    "queryModel": "AggStudentProfileQueryModel",
    "columns": [
      "student$caption",
      "student$studentNo",
      "overallLevel",
      "scoreTrend",
      "aiSummary"
    ],
    "slice": [
      { "field": "scoreTrend", "op": "=", "value": "declining" }
    ],
    "returnTotal": true
  }' | python -m json.tool
```

**预期结果**：筛出成绩趋势为"下降"的学生，及时发现问题。

---

## 场景 10：查看某学生的学习建议

**业务场景**：老师想查看某学生当前待处理的学习建议，按紧急程度排序

> 💬 **老师提问**
>
> - "张浩然目前有什么学习建议还没处理的"
> - "这个孩子的待办学习建议有哪些，紧急的排前面"
> - （变体）"数学科目现在还有多少条待处理的学习建议，按类型统计一下"

```bash
curl -s -X POST http://localhost:8090/api/semantic-layer/query \
  -H "Content-Type: application/json" \
  -d '{
    "queryModel": "FactLearningAdviceQueryModel",
    "columns": [
      "student$caption",
      "subject$caption",
      "knowledgePoint$caption",
      "adviceType",
      "adviceLevel",
      "adviceContent",
      "generateType",
      "status",
      "createdAt",
      "expiresAt"
    ],
    "slice": [
      { "field": "student$studentNo", "op": "=", "value": "20240101" },
      { "field": "status", "op": "=", "value": "pending" }
    ],
    "orderBy": [
      { "field": "adviceLevel", "dir": "asc" },
      { "field": "createdAt", "dir": "desc" }
    ]
  }' | python -m json.tool
```

**预期结果**：返回该学生所有待处理的学习建议，high（紧急）排最前面。

**变体 — 某科目所有学生的建议统计**：

```bash
curl -s -X POST http://localhost:8090/api/semantic-layer/query \
  -H "Content-Type: application/json" \
  -d '{
    "queryModel": "FactLearningAdviceQueryModel",
    "columns": [
      "subject$caption",
      "adviceType",
      "adviceLevel",
      "adviceCount"
    ],
    "slice": [
      { "field": "status", "op": "=", "value": "pending" },
      { "field": "subject$caption", "op": "=", "value": "数学" }
    ],
    "groupBy": [
      { "field": "subject$caption" },
      { "field": "adviceType" },
      { "field": "adviceLevel" }
    ],
    "calculatedFields": [
      {
        "name": "adviceCount",
        "caption": "建议数",
        "expression": "advice_id",
        "agg": "COUNT"
      }
    ],
    "orderBy": [
      { "field": "adviceCount", "dir": "desc" }
    ]
  }' | python -m json.tool
```

**预期结果**：按建议类型和紧急程度分组统计数学科目的待处理建议数量。

---

## 快速验证清单

在录制视频/写笔记时，建议按以下顺序演示，逻辑线为"**发现问题 → 分析原因 → 给出建议**"：

| 步骤 | 场景 | 亮点 |
|------|------|------|
| 1 | 场景 3：各科平均分 | 分组聚合 — 一眼看出哪科薄弱 |
| 2 | 场景 2：不及格学生 | 条件筛选 — 精准定位问题学生 |
| 3 | 场景 4：班级排名 | 窗口函数 — 实时排名不存储 |
| 4 | 场景 5：成绩趋势 | 时间序列 — 发现成绩下滑 |
| 5 | 场景 9 变体：下降画像 | AI 画像 — 综合评估学生状态 |
| 6 | 场景 10：学习建议 | AI 建议 — 闭环解决方案 |

---

## LLM 测试评估要点

用自然语言提问测试时，重点关注 LLM 能否正确处理以下挑战：

| 难度 | 挑战 | 示例 |
|------|------|------|
| ⭐ | 选对 queryModel | "考勤"→ AttendanceQM，"成绩"→ ScoreQM |
| ⭐ | 基本条件映射 | "不及格" → `score < 60` |
| ⭐⭐ | 口语化表述识别 | "挂科"="不及格"，"退步"="declining"，"请假"="sick_leave" |
| ⭐⭐ | 隐含字段推断 | "排名" → 需要生成 calculatedFields + RANK() 窗口函数 |
| ⭐⭐ | 模糊时间解析 | "上个月"→ 推算具体年月，"这次期中"→ 找最近的 midterm |
| ⭐⭐⭐ | 多模型串联 | "成绩下降的学生有什么学习建议" → 先查画像再查建议 |
| ⭐⭐⭐ | 指代消解 | "我们班"→ 需要上下文得知是哪个班 |
| ⭐⭐⭐ | 聚合意图识别 | "哪科考得最好" → 需要 groupBy + orderBy desc + limit 1 |

---

## 注意事项

1. **确保服务已启动**：运行 `start.bat` 或 `mvn spring-boot:run -DskipTests`
2. **确保有测试数据**：首次启动后需导入 seed data：
   ```bash
   docker exec -i student-analytics-mysql mysql -uroot -proot123 student_analytics < sql/seed-data.sql
   ```
3. **学号格式**：seed data 中学号格式为 `2024XXYY`（七年级）、`2023XXYY`（八年级）、`2022XXYY`（九年级），其中 XX 为班序号，YY 为学生序号。例：`20240101` = 七(1)班 1号 张浩然
4. **考试名称**：共 18 次考试，跨 3 个学期。期中考试可用 `like "期中"` 匹配，期末用 `exam$examType = "final"`
5. **Windows PowerShell**：如果在 PowerShell 中运行 curl，需要用 `curl.exe` 替代 `curl`（避免与 Invoke-WebRequest 别名冲突），或使用 Git Bash
6. **日期维度数据**：seed data 覆盖 2024-09-01 到 2026-01-31 的日期
