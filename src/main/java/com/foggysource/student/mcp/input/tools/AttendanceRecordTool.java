package com.foggysource.student.mcp.input.tools;

import com.foggysource.student.mcp.input.InputTool;
import com.foggysource.student.mcp.input.dto.InputToolResponse;
import com.foggysource.student.repository.BatchLogRepository;
import com.foggysource.student.repository.InputDataRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AttendanceRecordTool implements InputTool {

    private final InputDataRepository dataRepo;
    private final BatchLogRepository batchRepo;

    public AttendanceRecordTool(InputDataRepository dataRepo, BatchLogRepository batchRepo) {
        this.dataRepo = dataRepo;
        this.batchRepo = batchRepo;
    }

    @Override
    public String getName() { return "attendance.record"; }

    @Override
    public String getDescription() {
        return "记录学生考勤状态。支持批量记录多人同一日期的考勤。";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "student_nos", Map.of("type", "array", "description", "学号列表",
                                "items", Map.of("type", "string")),
                        "date", Map.of("type", "string", "description", "日期（YYYY-MM-DD），默认今天"),
                        "status", Map.of("type", "string", "description", "状态（absent/late/leave_early/sick_leave）"),
                        "time_slot", Map.of("type", "string", "description", "时段（morning/afternoon/evening），可选"),
                        "reason", Map.of("type", "string", "description", "原因说明，可选")
                ),
                "required", List.of("student_nos", "status")
        );
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public Object execute(Map<String, Object> args) {
        List<String> studentNos = (List<String>) args.get("student_nos");
        String dateStr = (String) args.get("date");
        String status = (String) args.get("status");
        String timeSlot = (String) args.get("time_slot");
        String reason = (String) args.get("reason");

        if (studentNos == null || studentNos.isEmpty()) {
            return InputToolResponse.error("学号列表为空");
        }

        // 1. 校验状态
        if (!List.of("absent", "late", "leave_early", "sick_leave").contains(status)) {
            return InputToolResponse.error("考勤状态无效：" + status,
                    "可选值：absent（缺勤）、late（迟到）、leave_early（早退）、sick_leave（病假）");
        }

        // 2. 校验时段
        if (timeSlot != null && !List.of("morning", "afternoon", "evening").contains(timeSlot)) {
            return InputToolResponse.error("时段无效：" + timeSlot,
                    "可选值：morning（上午）、afternoon（下午）、evening（晚上）");
        }

        // 3. 解析日期
        String dateId;
        if (dateStr != null) {
            try {
                LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                dateId = dateStr;
            } catch (DateTimeParseException e) {
                return InputToolResponse.error("日期格式无效：" + dateStr, "请使用 YYYY-MM-DD 格式");
            }
        } else {
            dateId = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        // 4. 批量解析学号
        List<Map<String, Object>> students = dataRepo.findStudentsByNos(studentNos);
        Map<String, Map<String, Object>> studentMap = students.stream()
                .collect(Collectors.toMap(s -> (String) s.get("student_no"), s -> s));

        // 5. 逐条处理
        int successCount = 0;
        int skipCount = 0;
        List<Map<String, Object>> errors = new ArrayList<>();
        List<String> successNames = new ArrayList<>();

        String statusLabel = switch (status) {
            case "absent" -> "缺勤";
            case "late" -> "迟到";
            case "leave_early" -> "早退";
            case "sick_leave" -> "病假";
            default -> status;
        };

        Long batchId = batchRepo.createBatch("attendance_record", "fact_attendance", 0,
                studentNos.size() + "人" + statusLabel + " | " + dateId);

        for (String studentNo : studentNos) {
            Map<String, Object> student = studentMap.get(studentNo);
            if (student == null) {
                errors.add(Map.of("student_no", studentNo, "reason", "学号不存在"));
                continue;
            }

            Long studentId = ((Number) student.get("student_id")).longValue();
            Long classId = ((Number) student.get("class_id")).longValue();

            // 检查重复
            if (dataRepo.findAttendance(studentId, dateId, timeSlot) != null) {
                skipCount++;
                continue;
            }

            dataRepo.insertAttendance(studentId, classId, dateId, status, timeSlot, reason, batchId);
            successCount++;
            successNames.add(student.get("student_name") + "(" + studentNo + ")");
        }

        String summary = "已记录 " + successCount + " 人考勤：" +
                String.join("、", successNames) + " " + statusLabel + " | " + dateId +
                (skipCount > 0 ? "，跳过 " + skipCount + " 条（已存在）" : "") +
                (!errors.isEmpty() ? "，失败 " + errors.size() + " 条" : "");

        return InputToolResponse.builder()
                .status("success")
                .batchId(batchId)
                .message("考勤记录完成")
                .summary(summary)
                .successCount(successCount)
                .skipCount(skipCount)
                .errorCount(errors.size())
                .errors(errors.isEmpty() ? null : errors)
                .build();
    }
}
