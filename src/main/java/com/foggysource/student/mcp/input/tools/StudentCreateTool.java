package com.foggysource.student.mcp.input.tools;

import com.foggysource.student.mcp.input.InputTool;
import com.foggysource.student.mcp.input.dto.InputToolResponse;
import com.foggysource.student.repository.BatchLogRepository;
import com.foggysource.student.repository.InputDataRepository;
import com.foggysource.student.service.ClassNameMatcher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.List;
import java.util.Map;

@Component
public class StudentCreateTool implements InputTool {

    private final InputDataRepository dataRepo;
    private final BatchLogRepository batchRepo;
    private final ClassNameMatcher classMatcher;

    public StudentCreateTool(InputDataRepository dataRepo, BatchLogRepository batchRepo, ClassNameMatcher classMatcher) {
        this.dataRepo = dataRepo;
        this.batchRepo = batchRepo;
        this.classMatcher = classMatcher;
    }

    @Override
    public String getName() { return "student.create"; }

    @Override
    public String getDescription() {
        return "新增一名学生到指定班级。学号可自动生成。";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "student_name", Map.of("type", "string", "description", "学生姓名"),
                        "class_name", Map.of("type", "string", "description", "班级名称（如「初一(1)班」）"),
                        "gender", Map.of("type", "string", "description", "性别（M/F），可选"),
                        "birth_date", Map.of("type", "string", "description", "出生日期（YYYY-MM-DD），可选"),
                        "student_no", Map.of("type", "string", "description", "学号，不传则自动生成"),
                        "phone", Map.of("type", "string", "description", "联系电话，可选")
                ),
                "required", List.of("student_name", "class_name")
        );
    }

    @Override
    @Transactional
    public Object execute(Map<String, Object> args) {
        String studentName = (String) args.get("student_name");
        String className = (String) args.get("class_name");
        String gender = (String) args.get("gender");
        String birthDateStr = (String) args.get("birth_date");
        String studentNo = (String) args.get("student_no");
        String phone = (String) args.get("phone");

        // 1. 匹配班级
        List<Map<String, Object>> allClasses = dataRepo.findAllClasses();
        Map<String, Object> cls = classMatcher.match(className, allClasses);
        if (cls == null) {
            List<String> classNames = allClasses.stream()
                    .map(c -> (String) c.get("class_name"))
                    .toList();
            return InputToolResponse.error("班级「" + className + "」未找到",
                    "可用班级：" + classNames);
        }

        Long classId = ((Number) cls.get("class_id")).longValue();
        String resolvedClassName = (String) cls.get("class_name");

        // 2. 如果未提供学号，自动生成
        if (studentNo == null || studentNo.isBlank()) {
            studentNo = generateStudentNo(classId);
        }

        // 3. 检查学号是否已存在
        if (dataRepo.findStudentByNo(studentNo) != null) {
            return InputToolResponse.error("学号 " + studentNo + " 已存在");
        }

        // 4. 解析出生日期
        Date birthDate = null;
        if (birthDateStr != null) {
            try {
                birthDate = Date.valueOf(birthDateStr);
            } catch (IllegalArgumentException e) {
                return InputToolResponse.error("出生日期格式无效：" + birthDateStr, "请使用 YYYY-MM-DD 格式");
            }
        }

        // 5. 校验性别
        if (gender != null && !gender.equals("M") && !gender.equals("F")) {
            return InputToolResponse.error("性别无效：" + gender, "可选值：M（男）、F（女）");
        }

        // 6. 创建批次并写入
        String summary = studentName + " | " + resolvedClassName + " | " + studentNo +
                (gender != null ? " | " + (gender.equals("M") ? "男" : "女") : "");
        Long batchId = batchRepo.createBatch("student_create", "dim_student", 1, summary);
        Long studentId = dataRepo.insertStudent(studentNo, studentName, gender, birthDate, classId, phone, batchId);

        // 7. 更新班级人数
        dataRepo.updateClassStudentCount(classId, 1);

        return InputToolResponse.success(batchId,
                "已添加：" + studentName + " | " + resolvedClassName + " | 学号 " + studentNo +
                        (gender != null ? " | " + (gender.equals("M") ? "男" : "女") : ""));
    }

    private String generateStudentNo(Long classId) {
        String maxNo = dataRepo.findMaxStudentNoByClassId(classId);
        if (maxNo != null && maxNo.length() >= 2) {
            try {
                long num = Long.parseLong(maxNo);
                return String.format("%0" + maxNo.length() + "d", num + 1);
            } catch (NumberFormatException e) {
                // 非纯数字学号，使用备选方案
            }
        }
        // 备选：classId + 01
        return String.format("%04d01", classId);
    }
}
