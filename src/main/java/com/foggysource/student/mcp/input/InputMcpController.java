package com.foggysource.student.mcp.input;

import com.foggyframework.dataset.mcp.schema.McpError;
import com.foggyframework.dataset.mcp.schema.McpRequest;
import com.foggyframework.dataset.mcp.schema.McpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * MCP 数据录入服务控制器。
 * 端点：/mcp/input/rpc
 * 独立于查询服务 /mcp/analyst/rpc。
 */
@RestController
@RequestMapping("/mcp/input")
public class InputMcpController {

    private static final Logger log = LoggerFactory.getLogger(InputMcpController.class);

    private final InputToolRegistry registry;

    public InputMcpController(InputToolRegistry registry) {
        this.registry = registry;
    }

    @PostMapping(value = "/rpc", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<McpResponse> handleRpc(@RequestBody McpRequest request) {
        String method = request.getMethod();
        if (method == null) {
            return ResponseEntity.badRequest()
                    .body(McpResponse.error(request.getId(), McpError.INVALID_REQUEST, "Missing method"));
        }

        try {
            McpResponse response = switch (method) {
                case "initialize" -> handleInitialize(request);
                case "ping" -> McpResponse.success(request.getId(), Map.of("status", "ok"));
                case "tools/list" -> handleToolsList(request);
                case "tools/call" -> handleToolsCall(request);
                default -> McpResponse.error(request.getId(), McpError.METHOD_NOT_FOUND,
                        "Unknown method: " + method);
            };
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("MCP input RPC error: method={}", method, e);
            return ResponseEntity.ok(McpResponse.error(request.getId(),
                    McpError.INTERNAL_ERROR, e.getMessage()));
        }
    }

    private McpResponse handleInitialize(McpRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", "2024-11-05");
        result.put("capabilities", Map.of("tools", Map.of("listChanged", false)));
        result.put("serverInfo", Map.of(
                "name", "student-analytics-input",
                "version", "0.1.0"
        ));
        return McpResponse.success(request.getId(), result);
    }

    private McpResponse handleToolsList(McpRequest request) {
        List<Map<String, Object>> tools = registry.getToolDefinitions();
        return McpResponse.success(request.getId(), Map.of("tools", tools));
    }

    @SuppressWarnings("unchecked")
    private McpResponse handleToolsCall(McpRequest request) {
        Map<String, Object> params = request.getParams();
        if (params == null || !params.containsKey("name")) {
            return McpResponse.error(request.getId(), McpError.INVALID_PARAMS, "Missing tool name");
        }

        String toolName = (String) params.get("name");
        Map<String, Object> arguments = params.containsKey("arguments")
                ? (Map<String, Object>) params.get("arguments")
                : Map.of();

        InputTool tool = registry.getTool(toolName);
        if (tool == null) {
            return McpResponse.error(request.getId(), McpError.TOOL_NOT_FOUND,
                    "Tool not found: " + toolName);
        }

        try {
            Object result = tool.execute(arguments);
            // MCP 协议要求 tools/call 返回 content 数组
            List<Map<String, Object>> content = new ArrayList<>();
            content.add(Map.of("type", "text", "text", result.toString()));
            return McpResponse.success(request.getId(), Map.of("content", content));
        } catch (Exception e) {
            log.error("Tool execution error: tool={}", toolName, e);
            List<Map<String, Object>> content = new ArrayList<>();
            content.add(Map.of("type", "text", "text", "错误：" + e.getMessage()));
            return McpResponse.success(request.getId(), Map.of("content", content, "isError", true));
        }
    }
}
