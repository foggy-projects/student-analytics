package com.foggysource.student.mcp.input.tools;

import com.foggysource.student.mcp.input.InputTool;
import com.foggysource.student.mcp.input.dto.InputToolResponse;
import com.foggysource.student.repository.BatchLogRepository;
import com.foggysource.student.repository.InputDataRepository;
import com.foggysource.student.repository.UpdateSnapshotRepository;
import com.foggysource.student.service.ScoreLevelCalculator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Component
public class ScoreUpdateTool implements InputTool {

    private final InputDataRepository dataRepo;
    private final BatchLogRepository batchRepo;
    private final UpdateSnapshotRepository snapshotRepo;
    private final ScoreLevelCalculator levelCalc;

    public ScoreUpdateTool(InputDataRepository dataRepo, BatchLogRepository batchRepo,
                           UpdateSnapshotRepository snapshotRepo, ScoreLevelCalculator levelCalc) {
        this.dataRepo = dataRepo;
        this.batchRepo = batchRepo;
        this.snapshotRepo = snapshotRepo;
        this.levelCalc = levelCalc;
    }

    @Override
    public String getName() { return "score.update"; }

    @Override
    public String getDescription() {
        return "修改已有的学生成绩。通过学号定位学生，未指定考试时自动定位最近一场。";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "student_no", Map.of("type", "string", "description", "学号"),
                        "subject_name", Map.of("type", "string", "description", "科目名称"),
                        "score", Map.of("type", "number", "description", "新分数"),
                        "exam_name", Map.of("type", "string", "description", "考试名称（可选，不传则自动定位）"),
                        "score_level", Map.of("type", "string", "description", "新等级（可选，自动计算）")
                ),
                "required", List.of("student_no", "subject_name", "score")
        );
    }

    @Override
    @Transactional
    public Object execute(Map<String, Object> args) {
        String studentNo = (String) args.get("student_no");
        String subjectName = (String) args.get("subject_name");
        BigDecimal newScore = new BigDecimal(args.get("score").toString());
        String examName = (String) args.get("exam_name");
        String scoreLevel = (String) args.get("score_level");

        // 1. 解析学生
        Map<String, Object> student = dataRepo.findStudentByNo(studentNo);
        if (student == null) {
            return InputToolResponse.notFound("学号 " + studentNo + " 未找到", null);
        }

        // 2. 解析科目
        Map<String, Object> subject = dataRepo.findSubjectByName(subjectName);
        if (subject == null) {
            return InputToolResponse.error("科目「" + subjectName + "」不存在");
        }

        Long studentId = ((Number) student.get("student_id")).longValue();
        Long subjectId = ((Number) subject.get("subject_id")).longValue();
        String studentName = (String) student.get("student_name");

        // 3. 定位成绩记录
        Map<String, Object> scoreRecord;
        if (examName != null) {
            Map<String, Object> exam = dataRepo.findExamByName(examName);
            if (exam == null) {
                return InputToolResponse.notFound("考试「" + examName + "」不存在", null);
            }
            scoreRecord = dataRepo.findScore(studentId, subjectId, ((Number) exam.get("exam_id")).longValue());
            if (scoreRecord == null) {
                return InputToolResponse.notFound("未找到该学生在该考试的" + subjectName + "成绩", null);
            }
        } else {
            // 未指定考试，查所有该学生该科目的成绩
            List<Map<String, Object>> scores = dataRepo.findScoresByStudentAndSubject(studentId, subjectId);
            if (scores.isEmpty()) {
                return InputToolResponse.notFound("未找到 " + studentName + " 的" + subjectName + "成绩记录", null);
            }
            if (scores.size() == 1) {
                scoreRecord = scores.get(0);
            } else {
                // 多条，返回候选让 AI 引导选择
                List<Map<String, Object>> candidates = new ArrayList<>();
                for (Map<String, Object> s : scores) {
                    candidates.add(Map.of(
                            "exam_name", s.get("exam_name"),
                            "exam_date", s.get("exam_date").toString(),
                            "current_score", s.get("score")
                    ));
                }
                return InputToolResponse.builder()
                        .status("ambiguous")
                        .message(studentName + " 有 " + scores.size() + " 条" + subjectName + "成绩记录，请指定考试名称")
                        .candidates(candidates)
                        .build();
            }
        }

        // 4. 计算新等级
        if (scoreLevel == null) {
            scoreLevel = levelCalc.calculate(newScore,
                    ((Number) subject.get("pass_score")).intValue(),
                    ((Number) subject.get("excellent_score")).intValue());
        }

        // 5. 创建批次和快照
        Long scoreId = ((Number) scoreRecord.get("score_id")).longValue();
        BigDecimal oldScore = (BigDecimal) scoreRecord.get("score");
        String oldLevel = (String) scoreRecord.get("score_level");

        String resolvedExamName = examName != null ? examName :
                (scoreRecord.containsKey("exam_name") ? (String) scoreRecord.get("exam_name") : "未知考试");

        String summary = studentName + "(" + studentNo + ") | " + resolvedExamName + " | " + subjectName +
                " | " + oldScore + " → " + newScore;
        Long batchId = batchRepo.createBatch("score_update", "fact_score", 1, summary);

        // 快照
        Map<String, Object> oldValues = Map.of("score", oldScore, "score_level", oldLevel != null ? oldLevel : "");
        Map<String, Object> newValues = Map.of("score", newScore, "score_level", scoreLevel);
        snapshotRepo.createSnapshot(batchId, "fact_score", scoreId, oldValues, newValues);

        // 6. 执行更新
        dataRepo.updateScore(scoreId, newScore, scoreLevel, batchId);

        return InputToolResponse.success(batchId,
                "已修改：" + studentName + "(" + studentNo + ") | " + resolvedExamName + " | " + subjectName +
                        " | " + oldScore + " → " + newScore + " | " + oldLevel + " → " + scoreLevel);
    }
}
