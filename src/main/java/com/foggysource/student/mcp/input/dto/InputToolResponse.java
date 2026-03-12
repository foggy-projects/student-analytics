package com.foggysource.student.mcp.input.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 录入工具统一响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InputToolResponse {

    private String status;   // success / error / ambiguous / not_found
    private Long batchId;
    private String message;
    private String summary;
    private String suggestion;
    private Integer successCount;
    private Integer skipCount;
    private Integer errorCount;
    private List<Map<String, Object>> candidates;
    private List<Map<String, Object>> errors;

    public static InputToolResponse success(Long batchId, String message) {
        return InputToolResponse.builder()
                .status("success").batchId(batchId).message(message).build();
    }

    public static InputToolResponse success(Long batchId, String message, String summary) {
        return InputToolResponse.builder()
                .status("success").batchId(batchId).message(message).summary(summary).build();
    }

    public static InputToolResponse error(String message) {
        return InputToolResponse.builder().status("error").message(message).build();
    }

    public static InputToolResponse error(String message, String suggestion) {
        return InputToolResponse.builder().status("error").message(message).suggestion(suggestion).build();
    }

    public static InputToolResponse notFound(String message, String suggestion) {
        return InputToolResponse.builder().status("not_found").message(message).suggestion(suggestion).build();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (status != null) sb.append("[").append(status).append("] ");
        if (message != null) sb.append(message);
        if (summary != null) sb.append("\n摘要：").append(summary);
        if (batchId != null) sb.append("\n批次号：").append(batchId);
        if (successCount != null) sb.append("\n成功：").append(successCount).append("条");
        if (skipCount != null && skipCount > 0) sb.append("，跳过：").append(skipCount).append("条");
        if (errorCount != null && errorCount > 0) sb.append("，失败：").append(errorCount).append("条");
        if (suggestion != null) sb.append("\n建议：").append(suggestion);
        if (candidates != null && !candidates.isEmpty()) {
            sb.append("\n候选列表：");
            for (Map<String, Object> c : candidates) sb.append("\n  · ").append(c);
        }
        if (errors != null && !errors.isEmpty()) {
            sb.append("\n错误详情：");
            for (Map<String, Object> e : errors) sb.append("\n  · ").append(e);
        }
        return sb.toString();
    }
}
