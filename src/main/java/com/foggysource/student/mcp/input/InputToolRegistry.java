package com.foggysource.student.mcp.input;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 录入工具注册表，管理所有 InputTool 实例。
 */
@Component
public class InputToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(InputToolRegistry.class);

    private final Map<String, InputTool> tools = new ConcurrentHashMap<>();

    public InputToolRegistry(List<InputTool> toolList) {
        for (InputTool tool : toolList) {
            tools.put(tool.getName(), tool);
            log.info("Registered input MCP tool: {}", tool.getName());
        }
        log.info("Total {} input MCP tools registered", tools.size());
    }

    public InputTool getTool(String name) {
        return tools.get(name);
    }

    public List<Map<String, Object>> getToolDefinitions() {
        List<Map<String, Object>> defs = new ArrayList<>();
        for (InputTool tool : tools.values()) {
            Map<String, Object> def = new LinkedHashMap<>();
            def.put("name", tool.getName());
            def.put("description", tool.getDescription());
            def.put("inputSchema", tool.getInputSchema());
            defs.add(def);
        }
        return defs;
    }
}
