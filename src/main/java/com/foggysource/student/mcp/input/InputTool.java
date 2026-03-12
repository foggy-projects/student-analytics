package com.foggysource.student.mcp.input;

import java.util.Map;

/**
 * MCP 录入工具接口。
 * 独立于 foggy-dataset-mcp 的 McpTool，避免被 analyst 端点自动发现。
 */
public interface InputTool {

    String getName();

    String getDescription();

    Map<String, Object> getInputSchema();

    Object execute(Map<String, Object> arguments);
}
