package com.foggysource.student.mcp.input.tools;

import com.foggysource.student.mcp.input.InputTool;
import com.foggysource.student.repository.BatchLogRepository;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class BatchListTool implements InputTool {

    private final BatchLogRepository batchRepo;

    public BatchListTool(BatchLogRepository batchRepo) {
        this.batchRepo = batchRepo;
    }

    @Override
    public String getName() { return "batch.list"; }

    @Override
    public String getDescription() {
        return "查询最近的操作批次记录，方便找到要回撤的批次号。";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "limit", Map.of("type", "number", "description", "返回条数，默认10"),
                        "status", Map.of("type", "string", "description", "过滤状态（committed/rolled_back），默认返回所有")
                )
        );
    }

    @Override
    public Object execute(Map<String, Object> args) {
        int limit = 10;
        String status = null;

        if (args.containsKey("limit") && args.get("limit") != null) {
            limit = ((Number) args.get("limit")).intValue();
        }
        if (args.containsKey("status") && args.get("status") != null) {
            status = (String) args.get("status");
        }

        List<Map<String, Object>> batches = batchRepo.listRecent(limit, status);

        if (batches.isEmpty()) {
            return Map.of("status", "success", "message", "暂无操作记录", "batches", List.of());
        }

        // 格式化输出
        StringBuilder sb = new StringBuilder();
        sb.append("最近 ").append(batches.size()).append(" 条操作记录：\n");
        for (Map<String, Object> b : batches) {
            String batchStatus = (String) b.get("status");
            String statusIcon = "committed".equals(batchStatus) ? "✅ 有效" : "❌ 已回撤";
            sb.append("批次 ").append(b.get("batch_id"))
                    .append(" | ").append(b.get("created_at"))
                    .append(" | ").append(translateBatchType((String) b.get("batch_type")))
                    .append(" | ").append(b.get("record_count")).append("条")
                    .append(" | ").append(statusIcon);
            if (b.get("summary") != null) {
                sb.append(" | ").append(b.get("summary"));
            }
            sb.append("\n");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "success");
        result.put("message", sb.toString().trim());
        result.put("total", batches.size());
        result.put("batches", batches);
        return result;
    }

    private String translateBatchType(String batchType) {
        if (batchType == null) return "未知";
        return switch (batchType) {
            case "score_create" -> "录入成绩";
            case "score_update" -> "修改成绩";
            case "score_batch_create" -> "批量录入成绩";
            case "student_create" -> "新增学生";
            case "exam_create" -> "创建考试";
            case "attendance_record" -> "记录考勤";
            default -> batchType;
        };
    }
}
