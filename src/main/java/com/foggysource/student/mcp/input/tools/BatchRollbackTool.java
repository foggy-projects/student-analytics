package com.foggysource.student.mcp.input.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foggysource.student.mcp.input.InputTool;
import com.foggysource.student.mcp.input.dto.InputToolResponse;
import com.foggysource.student.repository.BatchLogRepository;
import com.foggysource.student.repository.InputDataRepository;
import com.foggysource.student.repository.UpdateSnapshotRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class BatchRollbackTool implements InputTool {

    private final BatchLogRepository batchRepo;
    private final InputDataRepository dataRepo;
    private final UpdateSnapshotRepository snapshotRepo;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BatchRollbackTool(BatchLogRepository batchRepo, InputDataRepository dataRepo,
                             UpdateSnapshotRepository snapshotRepo, JdbcTemplate jdbc) {
        this.batchRepo = batchRepo;
        this.dataRepo = dataRepo;
        this.snapshotRepo = snapshotRepo;
        this.jdbc = jdbc;
    }

    @Override
    public String getName() { return "batch.rollback"; }

    @Override
    public String getDescription() {
        return "按批次号回撤一次录入操作。支持回撤成绩录入、修改、考勤、学生创建、考试创建等操作。";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "batch_id", Map.of("type", "number", "description", "批次号")
                ),
                "required", List.of("batch_id")
        );
    }

    @Override
    @Transactional
    public Object execute(Map<String, Object> args) {
        Long batchId = ((Number) args.get("batch_id")).longValue();

        // 1. 查找批次
        Map<String, Object> batch = batchRepo.findById(batchId);
        if (batch == null) {
            return InputToolResponse.notFound("批次 " + batchId + " 不存在", null);
        }

        String status = (String) batch.get("status");
        if ("rolled_back".equals(status)) {
            return InputToolResponse.error("批次 " + batchId + " 已回撤，不可重复操作");
        }

        String batchType = (String) batch.get("batch_type");
        String summary = (String) batch.get("summary");
        int affectedCount = 0;

        // 2. 根据类型执行回撤
        switch (batchType) {
            case "score_create", "score_batch_create" -> {
                affectedCount = dataRepo.countScoresByBatchId(batchId);
                dataRepo.deleteScoresByBatchId(batchId);
            }
            case "score_update" -> {
                affectedCount = restoreScoreSnapshots(batchId);
            }
            case "student_create" -> {
                // 需要先统计各班级的回退数量，再删除
                List<Map<String, Object>> countByClass = jdbc.queryForList(
                        "SELECT class_id, COUNT(*) as cnt FROM dim_student WHERE batch_id = ? GROUP BY class_id",
                        batchId);
                for (Map<String, Object> row : countByClass) {
                    Long classId = ((Number) row.get("class_id")).longValue();
                    int cnt = ((Number) row.get("cnt")).intValue();
                    dataRepo.updateClassStudentCount(classId, -cnt);
                }
                affectedCount = countByClass.stream()
                        .mapToInt(r -> ((Number) r.get("cnt")).intValue()).sum();
                dataRepo.deleteStudentsByBatchId(batchId);
            }
            case "exam_create" -> {
                // 检查是否有关联成绩
                List<Map<String, Object>> exams = jdbc.queryForList(
                        "SELECT exam_id FROM dim_exam WHERE batch_id = ?", batchId);
                for (Map<String, Object> exam : exams) {
                    Long examId = ((Number) exam.get("exam_id")).longValue();
                    if (dataRepo.examHasScores(examId)) {
                        return InputToolResponse.error(
                                "考试已有关联成绩，无法回撤",
                                "请先回撤相关成绩批次，再回撤考试创建");
                    }
                }
                affectedCount = exams.size();
                dataRepo.deleteExamsByBatchId(batchId);
            }
            case "attendance_record" -> {
                affectedCount = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM fact_attendance WHERE batch_id = ?",
                        Integer.class, batchId);
                dataRepo.deleteAttendanceByBatchId(batchId);
            }
            default -> {
                return InputToolResponse.error("不支持回撤的批次类型：" + batchType);
            }
        }

        // 3. 标记批次为已回撤
        batchRepo.markRolledBack(batchId);

        return InputToolResponse.success(batchId,
                "批次 " + batchId + " 已回撤：删除了 " + affectedCount + " 条记录" +
                        (summary != null ? "（" + summary + "）" : ""));
    }

    private int restoreScoreSnapshots(Long batchId) {
        List<Map<String, Object>> snapshots = snapshotRepo.findByBatchId(batchId);
        int count = 0;
        for (Map<String, Object> snapshot : snapshots) {
            Long recordId = ((Number) snapshot.get("record_id")).longValue();
            try {
                String oldValuesJson = snapshot.get("old_values").toString();
                Map<String, Object> oldValues = objectMapper.readValue(oldValuesJson,
                        new TypeReference<Map<String, Object>>() {});

                BigDecimal oldScore = new BigDecimal(oldValues.get("score").toString());
                String oldLevel = oldValues.get("score_level") != null ?
                        oldValues.get("score_level").toString() : null;
                if (oldLevel != null && oldLevel.isEmpty()) oldLevel = null;

                dataRepo.updateScore(recordId, oldScore, oldLevel, null);
                count++;
            } catch (Exception e) {
                throw new RuntimeException("Failed to restore snapshot for record " + recordId, e);
            }
        }
        return count;
    }
}
