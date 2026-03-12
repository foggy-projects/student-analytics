package com.foggysource.student.mcp.input.tools;

import com.foggysource.student.mcp.input.InputTool;
import com.foggysource.student.mcp.input.dto.InputToolResponse;
import com.foggysource.student.repository.BatchLogRepository;
import com.foggysource.student.repository.InputDataRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.List;
import java.util.Map;

@Component
public class ExamCreateTool implements InputTool {

    private final InputDataRepository dataRepo;
    private final BatchLogRepository batchRepo;

    public ExamCreateTool(InputDataRepository dataRepo, BatchLogRepository batchRepo) {
        this.dataRepo = dataRepo;
        this.batchRepo = batchRepo;
    }

    @Override
    public String getName() { return "exam.create"; }

    @Override
    public String getDescription() {
        return "新建一场考试记录。录入成绩前需要先有考试。";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "exam_name", Map.of("type", "string", "description", "考试名称"),
                        "exam_type", Map.of("type", "string", "description", "考试类型（daily/unit/midterm/final）"),
                        "exam_date", Map.of("type", "string", "description", "考试日期（YYYY-MM-DD）"),
                        "semester_name", Map.of("type", "string", "description", "所属学期名称，不传则根据日期自动匹配")
                ),
                "required", List.of("exam_name", "exam_type", "exam_date")
        );
    }

    @Override
    @Transactional
    public Object execute(Map<String, Object> args) {
        String examName = (String) args.get("exam_name");
        String examType = (String) args.get("exam_type");
        String examDateStr = (String) args.get("exam_date");
        String semesterName = (String) args.get("semester_name");

        // 1. 检查考试名称是否已存在
        if (dataRepo.findExamByName(examName) != null) {
            return InputToolResponse.error("考试「" + examName + "」已存在");
        }

        // 2. 校验考试类型
        if (!List.of("daily", "unit", "midterm", "final").contains(examType)) {
            return InputToolResponse.error("考试类型无效：" + examType,
                    "可选类型：daily（随堂测验）、unit（单元测试）、midterm（期中考试）、final（期末考试）");
        }

        // 3. 解析日期
        Date examDate;
        try {
            examDate = Date.valueOf(examDateStr);
        } catch (IllegalArgumentException e) {
            return InputToolResponse.error("日期格式无效：" + examDateStr, "请使用 YYYY-MM-DD 格式");
        }

        // 4. 解析学期
        Map<String, Object> semester = null;
        if (semesterName != null) {
            semester = dataRepo.findSemesterByName(semesterName);
            if (semester == null) {
                return InputToolResponse.error("学期「" + semesterName + "」未找到");
            }
        } else {
            // 根据日期自动匹配
            semester = dataRepo.findSemesterByDate(examDate);
            if (semester == null) {
                // 取当前学期
                semester = dataRepo.findCurrentSemester();
            }
        }

        if (semester == null) {
            return InputToolResponse.error("无法确定所属学期", "请指定 semester_name 参数");
        }

        Long semesterId = ((Number) semester.get("semester_id")).longValue();
        String resolvedSemesterName = (String) semester.get("semester_name");

        // 5. 创建批次并写入
        String summary = examName + " | " + examType + " | " + examDateStr + " | " + resolvedSemesterName;
        Long batchId = batchRepo.createBatch("exam_create", "dim_exam", 1, summary);
        Long examId = dataRepo.insertExam(examName, examType, examDate, semesterId, batchId);

        return InputToolResponse.success(batchId,
                "已创建考试：" + examName + " | " + examType + " | " + examDateStr + " | " + resolvedSemesterName);
    }
}
