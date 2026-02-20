-- ============================================================
-- Student Analytics - 数据库 Schema (一期 MVP)
-- 数据库：MySQL 5.7+
-- ============================================================

CREATE DATABASE IF NOT EXISTS student_analytics
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE student_analytics;

-- ============================================================
-- 权限组织域
-- ============================================================

CREATE TABLE dim_user (
    user_id       BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_name     VARCHAR(50)  NOT NULL COMMENT '用户名',
    password_hash VARCHAR(200) NOT NULL COMMENT '密码哈希',
    user_type     VARCHAR(20)  NOT NULL COMMENT '用户类型：admin/teacher',
    teacher_id    BIGINT       NULL     COMMENT '关联教师ID',
    status        VARCHAR(20)  NOT NULL DEFAULT 'active' COMMENT '状态：active/inactive',
    last_login_at TIMESTAMP    NULL     COMMENT '最后登录时间',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_name (user_name)
) COMMENT '用户表';

CREATE TABLE dim_teacher (
    teacher_id   BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    teacher_no   VARCHAR(20) NULL     COMMENT '工号',
    teacher_name VARCHAR(50) NOT NULL COMMENT '姓名',
    gender       CHAR(1)     NULL     COMMENT '性别：M/F',
    phone        VARCHAR(20) NULL     COMMENT '联系电话',
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '教师表';

CREATE TABLE fact_teacher_assignment (
    assignment_id   BIGINT    NOT NULL AUTO_INCREMENT PRIMARY KEY,
    teacher_id      BIGINT    NOT NULL COMMENT '教师ID',
    class_id        BIGINT    NOT NULL COMMENT '班级ID',
    subject_id      BIGINT    NULL     COMMENT '科目ID（班主任可为空）',
    is_head_teacher BOOLEAN   NOT NULL DEFAULT FALSE COMMENT '是否班主任',
    semester_id     BIGINT    NOT NULL COMMENT '学期ID',
    is_current      BOOLEAN   NOT NULL DEFAULT TRUE COMMENT '是否当前',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_teacher (teacher_id),
    INDEX idx_class (class_id),
    INDEX idx_semester (semester_id),
    INDEX idx_current (is_current, teacher_id)
) COMMENT '教师任课关系表';

-- ============================================================
-- 时间域
-- ============================================================

CREATE TABLE dim_semester (
    semester_id   BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    semester_name VARCHAR(50) NOT NULL COMMENT '学期名称',
    school_year   VARCHAR(20) NOT NULL COMMENT '学年，如 2025-2026',
    semester_type VARCHAR(10) NOT NULL COMMENT '学期类型：first/second',
    start_date    DATE        NOT NULL COMMENT '开始日期',
    end_date      DATE        NOT NULL COMMENT '结束日期',
    is_current    BOOLEAN     NOT NULL DEFAULT FALSE COMMENT '是否当前学期',
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '学期表';

CREATE TABLE dim_date (
    date_id      VARCHAR(10) NOT NULL PRIMARY KEY COMMENT '日期ID，格式：2024-01-15',
    date_value   DATE        NOT NULL COMMENT '日期值',
    year         INT         NOT NULL,
    month        INT         NOT NULL,
    day          INT         NOT NULL,
    week_day     INT         NOT NULL COMMENT '周几 1-7',
    week_of_year INT         NULL     COMMENT '年中第几周',
    semester_id  BIGINT      NULL     COMMENT '学期ID',
    is_school_day BOOLEAN    NOT NULL DEFAULT TRUE COMMENT '是否教学日',
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_semester (semester_id),
    INDEX idx_date_value (date_value)
) COMMENT '日期维度表';

-- ============================================================
-- 学生班级域
-- ============================================================

CREATE TABLE dim_grade (
    grade_id    BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    grade_name  VARCHAR(50) NOT NULL COMMENT '年级名称',
    grade_level INT         NULL     COMMENT '年级序号：1-6小学，7-9初中',
    stage       VARCHAR(20) NOT NULL COMMENT '学段：primary/junior',
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '年级表';

CREATE TABLE dim_class (
    class_id      BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    class_name    VARCHAR(50) NOT NULL COMMENT '班级名称',
    grade_id      BIGINT      NOT NULL COMMENT '年级ID',
    classroom     VARCHAR(50) NULL     COMMENT '教室',
    student_count INT         NULL     COMMENT '班级人数',
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_grade (grade_id)
) COMMENT '班级表';

CREATE TABLE dim_student (
    student_id     BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_no     VARCHAR(20) NULL     COMMENT '学号',
    student_name   VARCHAR(50) NOT NULL COMMENT '姓名',
    gender         CHAR(1)     NULL     COMMENT '性别：M/F',
    birth_date     DATE        NULL     COMMENT '出生日期',
    class_id       BIGINT      NOT NULL COMMENT '所属班级ID',
    enroll_date    DATE        NULL     COMMENT '入学日期',
    phone          VARCHAR(20) NULL     COMMENT '联系电话',
    student_status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态：active/graduated/transferred',
    created_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_class (class_id),
    INDEX idx_status (student_status)
) COMMENT '学生表';

-- ============================================================
-- 科目考试域
-- ============================================================

CREATE TABLE dim_subject (
    subject_id      BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    subject_name    VARCHAR(50) NOT NULL COMMENT '科目名称',
    subject_type    VARCHAR(20) NOT NULL COMMENT '科目类型：main/minor',
    full_score      INT         NOT NULL DEFAULT 100 COMMENT '满分',
    pass_score      INT         NOT NULL DEFAULT 60 COMMENT '及格分',
    excellent_score INT         NOT NULL DEFAULT 85 COMMENT '优秀分',
    is_exam_subject BOOLEAN     NOT NULL DEFAULT TRUE COMMENT '是否考试科目',
    sort_order      INT         NULL     COMMENT '排序',
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '科目表';

CREATE TABLE dim_exam (
    exam_id     BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    exam_name   VARCHAR(100) NOT NULL COMMENT '考试名称',
    exam_type   VARCHAR(20)  NOT NULL COMMENT '考试类型：daily/unit/midterm/final',
    exam_date   DATE         NOT NULL COMMENT '考试日期',
    semester_id BIGINT       NOT NULL COMMENT '学期ID',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_semester (semester_id),
    INDEX idx_type (exam_type),
    INDEX idx_date (exam_date)
) COMMENT '考试表';

CREATE TABLE dim_knowledge_point (
    point_id    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    point_code  VARCHAR(50)  NULL     COMMENT '知识点编码',
    point_name  VARCHAR(100) NOT NULL COMMENT '知识点名称',
    subject_id  BIGINT       NOT NULL COMMENT '所属科目ID',
    grade_id    BIGINT       NULL     COMMENT '适用年级ID',
    parent_id   BIGINT       NULL     COMMENT '父知识点ID',
    point_level INT          NULL     COMMENT '层级深度',
    difficulty  INT          NULL     COMMENT '难度等级 1-5',
    importance  INT          NULL     COMMENT '重要程度 1-5',
    description TEXT         NULL     COMMENT '知识点描述',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_subject (subject_id),
    INDEX idx_parent (parent_id)
) COMMENT '知识点表';

CREATE TABLE knowledge_point_closure (
    parent_id BIGINT NOT NULL COMMENT '祖先节点ID',
    point_id  BIGINT NOT NULL COMMENT '后代节点ID',
    distance  INT    NOT NULL COMMENT '层级距离，0表示自身',
    PRIMARY KEY (parent_id, point_id),
    INDEX idx_point (point_id)
) COMMENT '知识点闭包表 - 语义层层级聚合必需';

-- ============================================================
-- 成绩分析域
-- ============================================================

CREATE TABLE fact_score (
    score_id    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id  BIGINT       NOT NULL COMMENT '学生ID',
    class_id    BIGINT       NOT NULL COMMENT '班级ID',
    subject_id  BIGINT       NOT NULL COMMENT '科目ID',
    exam_id     BIGINT       NOT NULL COMMENT '考试ID',
    score       DECIMAL(5,2) NOT NULL COMMENT '得分',
    score_level CHAR(1)      NULL     COMMENT '成绩等级：A/B/C/D',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_student (student_id),
    INDEX idx_exam (exam_id),
    INDEX idx_class_subject (class_id, subject_id),
    INDEX idx_student_subject (student_id, subject_id)
) COMMENT '成绩事实表（排名通过 QM 窗口函数实时计算）';

CREATE TABLE agg_student_profile (
    profile_id    BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id    BIGINT      NOT NULL COMMENT '学生ID',
    overall_level VARCHAR(10) NULL     COMMENT '综合等级：A/B/C/D',
    score_trend   VARCHAR(20) NULL     COMMENT '成绩趋势：rising/stable/declining',
    ai_summary    TEXT        NULL     COMMENT 'AI 生成的完整画像 JSON',
    refreshed_at  TIMESTAMP   NULL     COMMENT '最后刷新时间',
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_student (student_id)
) COMMENT '学生能力画像聚合表（由 fact 数据衍生，定期刷新）';

CREATE TABLE fact_learning_advice (
    advice_id      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id     BIGINT       NOT NULL COMMENT '学生ID',
    subject_id     BIGINT       NULL     COMMENT '科目ID',
    point_id       BIGINT       NULL     COMMENT '知识点ID',
    advice_type    VARCHAR(50)  NOT NULL COMMENT '建议类型：review/practice/consolidate/extend',
    advice_level   VARCHAR(20)  NOT NULL COMMENT '紧急程度：high/medium/low',
    advice_content TEXT         NOT NULL COMMENT '建议内容',
    generate_type  VARCHAR(20)  NULL     COMMENT '生成方式：ai/rule',
    status         VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT '状态：pending/done/ignored',
    feedback       VARCHAR(500) NULL     COMMENT '老师反馈',
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at     TIMESTAMP    NULL     COMMENT '建议有效期',
    INDEX idx_student (student_id),
    INDEX idx_status (status)
) COMMENT '学习建议表';

-- ============================================================
-- 考勤域
-- ============================================================

CREATE TABLE fact_attendance (
    attendance_id BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id    BIGINT       NOT NULL COMMENT '学生ID',
    class_id      BIGINT       NOT NULL COMMENT '班级ID',
    date_id       VARCHAR(10)  NOT NULL COMMENT '日期ID',
    status        VARCHAR(20)  NOT NULL COMMENT '状态：present/absent/late/leave_early/sick_leave',
    time_slot     VARCHAR(20)  NULL     COMMENT '时段：morning/afternoon/evening',
    reason        VARCHAR(200) NULL     COMMENT '原因',
    recorded_by   BIGINT       NULL     COMMENT '记录人ID',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_student (student_id),
    INDEX idx_class_date (class_id, date_id),
    INDEX idx_status (status)
) COMMENT '考勤事实表';
