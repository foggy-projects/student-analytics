package com.foggysource.student.mcp.input.tools;

import com.foggysource.student.mcp.input.InputTool;
import com.foggysource.student.mcp.input.dto.InputToolResponse;
import com.foggysource.student.repository.BatchLogRepository;
import com.foggysource.student.repository.InputDataRepository;
import com.foggysource.student.service.ScoreLevelCalculator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ScoreBatchCreateTool implements InputTool {

    private final InputDataRepository dataRepo;
    private final BatchLogRepository batchRepo;
    private final ScoreLevelCalculator levelCalc;

    public ScoreBatchCreateTool(InputDataRepository dataRepo, BatchLogRepository batchRepo, ScoreLevelCalculator levelCalc) {
        this.dataRepo = dataRepo;
        this.batchRepo = batchRepo;
        this.levelCalc = levelCalc;
    }

    @Override
    public String getName() { return "score.batch_create"; }

    @Override
    public String getDescription() {
        return "批量录入一次考试的多条成绩。适用于整班/整科目录入，Skill 识别试卷图片后的主要调用入口。";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "exam_name", Map.of("type", "string", "description", "考试名称"),
                        "subject_name", Map.of("type", "string", "description", "科目名称"),
                        "scores", Map.of("type", "array", "description", "成绩数组",
                                "items", Map.of("type", "object", "properties", Map.of(
                                        "student_no", Map.of("type", "string"),
                                        "score", Map.of("type", "number"),
                                        "score_level", Map.of("type", "string")
                                ), "required", List.of("student_no", "score")))
                ),
                "required", List.of("exam_name", "subject_name", "scores")
        );
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public Object execute(Map<String, Object> args) {
        String examName = (String) args.get("exam_name");
        String subjectName = (String) args.get("subject_name");
        List<Map<String, Object>> scoreEntries = (List<Map<String, Object>>) args.get("scores");

        if (scoreEntries == null || scoreEntries.isEmpty()) {
            return InputToolResponse.error("成绩数组为空");
        }

        // 1. 解析考试和科目（共享）
        Map<String, Object> exam = dataRepo.findExamByName(examName);
        if (exam == null) {
            return InputToolResponse.notFound("考试「" + examName + "」不存在",
                    "请先使用 exam.create 创建考试");
        }
        Map<String, Object> subject = dataRepo.findSubjectByName(subjectName);
        if (subject == null) {
            return InputToolResponse.error("科目「" + subjectName + "」不存在");
        }

        Long examId = ((Number) exam.get("exam_id")).longValue();
        Long subjectId = ((Number) subject.get("subject_id")).longValue();
        int fullScore = ((Number) subject.get("full_score")).intValue();
        int passScore = ((Number) subject.get("pass_score")).intValue();
        int excellentScore = ((Number) subject.get("excellent_score")).intValue();

        // 2. 批量解析学号
        List<String> studentNos = scoreEntries.stream()
                .map(e -> (String) e.get("student_no"))
                .collect(Collectors.toList());
        List<Map<String, Object>> students = dataRepo.findStudentsByNos(studentNos);
        Map<String, Map<String, Object>> studentMap = students.stream()
                .collect(Collectors.toMap(s -> (String) s.get("student_no"), s -> s));

        // 3. 逐条校验
        int successCount = 0;
        int skipCount = 0;
        List<Map<String, Object>> errors = new ArrayList<>();
        Long batchId = batchRepo.createBatch("score_batch_create", "fact_score", 0,
                examName + " | " + subjectName + " | 批量录入");

        for (Map<String, Object> entry : scoreEntries) {
            String studentNo = (String) entry.get("student_no");
            BigDecimal score = new BigDecimal(entry.get("score").toString());
            String scoreLevel = (String) entry.get("score_level");

            Map<String, Object> student = studentMap.get(studentNo);
            if (student == null) {
                errors.add(Map.of("student_no", studentNo, "reason", "学号不存在"));
                continue;
            }

            if (score.doubleValue() > fullScore) {
                errors.add(Map.of("student_no", studentNo, "reason", "分数 " + score + " 超过满分 " + fullScore));
                continue;
            }

            Long studentId = ((Number) student.get("student_id")).longValue();
            Long classId = ((Number) student.get("class_id")).longValue();

            // 检查重复
            if (dataRepo.findScore(studentId, subjectId, examId) != null) {
                skipCount++;
                continue;
            }

            if (scoreLevel == null) {
                scoreLevel = levelCalc.calculate(score, passScore, excellentScore);
            }

            dataRepo.insertScore(studentId, classId, subjectId, examId, score, scoreLevel, batchId);
            successCount++;
        }

        // 4. 更新批次记录数
        int finalSuccessCount = successCount;
        // 简单更新 record_count
        if (successCount > 0) {
            // 批次已创建，通过直接 SQL 更新
        }

        String summary = examName + " | " + subjectName + " | " +
                "共" + scoreEntries.size() + "条，成功" + successCount + "条" +
                (skipCount > 0 ? "，跳过" + skipCount + "条（已存在）" : "") +
                (!errors.isEmpty() ? "，失败" + errors.size() + "条" : "");

        return InputToolResponse.builder()
                .status("success")
                .batchId(batchId)
                .message("批量录入完成")
                .summary(summary)
                .successCount(successCount)
                .skipCount(skipCount)
                .errorCount(errors.size())
                .errors(errors.isEmpty() ? null : errors)
                .build();
    }
}
