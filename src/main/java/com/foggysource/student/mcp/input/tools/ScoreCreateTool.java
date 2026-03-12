package com.foggysource.student.mcp.input.tools;

import com.foggysource.student.mcp.input.InputTool;
import com.foggysource.student.mcp.input.dto.InputToolResponse;
import com.foggysource.student.repository.BatchLogRepository;
import com.foggysource.student.repository.InputDataRepository;
import com.foggysource.student.service.ScoreLevelCalculator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ScoreCreateTool implements InputTool {

    private final InputDataRepository dataRepo;
    private final BatchLogRepository batchRepo;
    private final ScoreLevelCalculator levelCalc;

    public ScoreCreateTool(InputDataRepository dataRepo, BatchLogRepository batchRepo, ScoreLevelCalculator levelCalc) {
        this.dataRepo = dataRepo;
        this.batchRepo = batchRepo;
        this.levelCalc = levelCalc;
    }

    @Override
    public String getName() { return "score.create"; }

    @Override
    public String getDescription() {
        return "录入单条学生成绩。通过学号精确定位学生，写入成绩记录。成绩等级（A/B/C/D）可自动计算。";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "student_no", Map.of("type", "string", "description", "学号（唯一标识）"),
                        "subject_name", Map.of("type", "string", "description", "科目名称"),
                        "exam_name", Map.of("type", "string", "description", "考试名称"),
                        "score", Map.of("type", "number", "description", "分数"),
                        "score_level", Map.of("type", "string", "description", "成绩等级（A/B/C/D），不传则自动计算")
                ),
                "required", List.of("student_no", "subject_name", "exam_name", "score")
        );
    }

    @Override
    @Transactional
    public Object execute(Map<String, Object> args) {
        String studentNo = (String) args.get("student_no");
        String subjectName = (String) args.get("subject_name");
        String examName = (String) args.get("exam_name");
        BigDecimal score = new BigDecimal(args.get("score").toString());
        String scoreLevel = (String) args.get("score_level");

        // 1. 解析学生
        Map<String, Object> student = dataRepo.findStudentByNo(studentNo);
        if (student == null) {
            return InputToolResponse.notFound("学号 " + studentNo + " 未找到",
                    "请检查学号，或使用 student.create 创建新学生");
        }

        // 2. 解析科目
        Map<String, Object> subject = dataRepo.findSubjectByName(subjectName);
        if (subject == null) {
            List<Map<String, Object>> allSubjects = dataRepo.findAllSubjects();
            return InputToolResponse.error("科目「" + subjectName + "」不存在",
                    "可用科目：" + allSubjects.stream().map(s -> s.get("subject_name").toString()).toList());
        }

        int fullScore = ((Number) subject.get("full_score")).intValue();
        if (score.doubleValue() > fullScore) {
            return InputToolResponse.error("分数 " + score + " 超过满分 " + fullScore);
        }

        // 3. 解析考试
        Map<String, Object> exam = dataRepo.findExamByName(examName);
        if (exam == null) {
            return InputToolResponse.notFound("考试「" + examName + "」不存在",
                    "请使用 exam.create 创建考试");
        }

        Long studentId = ((Number) student.get("student_id")).longValue();
        Long classId = ((Number) student.get("class_id")).longValue();
        Long subjectId = ((Number) subject.get("subject_id")).longValue();
        Long examId = ((Number) exam.get("exam_id")).longValue();

        // 4. 检查重复
        if (dataRepo.findScore(studentId, subjectId, examId) != null) {
            return InputToolResponse.error("该成绩已存在", "请使用 score.update 修改成绩");
        }

        // 5. 计算等级
        if (scoreLevel == null) {
            scoreLevel = levelCalc.calculate(score,
                    ((Number) subject.get("pass_score")).intValue(),
                    ((Number) subject.get("excellent_score")).intValue());
        }

        // 6. 创建批次并写入
        String studentName = (String) student.get("student_name");
        String summary = studentName + "(" + studentNo + ") | " + examName + " | " + subjectName + " | " + score + "分 | " + scoreLevel;
        Long batchId = batchRepo.createBatch("score_create", "fact_score", 1, summary);
        dataRepo.insertScore(studentId, classId, subjectId, examId, score, scoreLevel, batchId);

        return InputToolResponse.success(batchId, "已录入：" + summary);
    }
}
