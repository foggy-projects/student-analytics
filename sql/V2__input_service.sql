-- ============================================================
-- V2: MCP 数据录入服务 - 批次追踪与回撤支持
-- ============================================================

USE student_analytics;

-- 批次日志表
CREATE TABLE IF NOT EXISTS sys_batch_log (
    batch_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_type     VARCHAR(30)  NOT NULL COMMENT '操作类型: score_create/score_update/score_batch_create/attendance_record/student_create/exam_create',
    target_table   VARCHAR(50)  NOT NULL COMMENT '目标表: fact_score/fact_attendance/dim_student/dim_exam',
    record_count   INT          NOT NULL COMMENT '影响记录数',
    status         VARCHAR(20)  NOT NULL DEFAULT 'committed' COMMENT 'committed/rolled_back',
    operator       VARCHAR(50)  NULL     COMMENT '操作来源标识',
    summary        VARCHAR(500) NULL     COMMENT '操作摘要',
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    rolled_back_at TIMESTAMP    NULL,
    INDEX idx_status (status),
    INDEX idx_created (created_at)
) COMMENT '数据录入批次日志';

-- 修改快照表
CREATE TABLE IF NOT EXISTS sys_update_snapshot (
    snapshot_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id     BIGINT      NOT NULL,
    target_table VARCHAR(50) NOT NULL,
    record_id    BIGINT      NOT NULL COMMENT '被修改记录的主键',
    old_values   JSON        NOT NULL COMMENT '修改前的字段值',
    new_values   JSON        NOT NULL COMMENT '修改后的字段值',
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_batch (batch_id)
) COMMENT '数据修改快照（用于回撤）';

-- 为已有表添加 batch_id 列（幂等）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='student_analytics' AND TABLE_NAME='fact_score' AND COLUMN_NAME='batch_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE fact_score ADD COLUMN batch_id BIGINT NULL COMMENT ''批次ID'', ADD INDEX idx_batch (batch_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='student_analytics' AND TABLE_NAME='fact_attendance' AND COLUMN_NAME='batch_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE fact_attendance ADD COLUMN batch_id BIGINT NULL COMMENT ''批次ID'', ADD INDEX idx_batch (batch_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='student_analytics' AND TABLE_NAME='dim_student' AND COLUMN_NAME='batch_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE dim_student ADD COLUMN batch_id BIGINT NULL COMMENT ''批次ID'', ADD INDEX idx_batch (batch_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='student_analytics' AND TABLE_NAME='dim_exam' AND COLUMN_NAME='batch_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE dim_exam ADD COLUMN batch_id BIGINT NULL COMMENT ''批次ID'', ADD INDEX idx_batch (batch_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
