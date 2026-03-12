package com.foggysource.student.mcp.input;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InputToolRegistryTest {

    @Test
    void testRegistryCollectsTools() {
        InputTool mockTool1 = createMockTool("test.tool1", "Test Tool 1");
        InputTool mockTool2 = createMockTool("test.tool2", "Test Tool 2");

        InputToolRegistry registry = new InputToolRegistry(List.of(mockTool1, mockTool2));

        assertThat(registry.getTool("test.tool1")).isNotNull();
        assertThat(registry.getTool("test.tool2")).isNotNull();
        assertThat(registry.getTool("nonexistent")).isNull();
    }

    @Test
    void testGetToolDefinitions() {
        InputTool mockTool = createMockTool("test.tool", "Test Tool");
        InputToolRegistry registry = new InputToolRegistry(List.of(mockTool));

        List<Map<String, Object>> defs = registry.getToolDefinitions();
        assertThat(defs).hasSize(1);
        assertThat(defs.get(0).get("name")).isEqualTo("test.tool");
        assertThat(defs.get(0).get("description")).isEqualTo("Test Tool");
    }

    private InputTool createMockTool(String name, String desc) {
        return new InputTool() {
            @Override public String getName() { return name; }
            @Override public String getDescription() { return desc; }
            @Override public Map<String, Object> getInputSchema() { return Map.of("type", "object"); }
            @Override public Object execute(Map<String, Object> args) { return null; }
        };
    }
}
