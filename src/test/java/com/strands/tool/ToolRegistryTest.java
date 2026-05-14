package com.strands.tool;

import com.strands.types.ToolResult;
import com.strands.types.ToolSpec;
import com.strands.types.ToolUse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ToolRegistryTest {

    private ToolRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ToolRegistry();
    }

    @Test
    void testRegisterAndGet() {
        AgentTool tool = createTool("greet", "Greets a person");
        registry.register(tool);

        assertEquals(tool, registry.get("greet"));
        assertTrue(registry.contains("greet"));
        assertEquals(1, registry.size());
    }

    @Test
    void testGetNonExistent() {
        assertNull(registry.get("nonexistent"));
        assertFalse(registry.contains("nonexistent"));
    }

    @Test
    void testRemove() {
        registry.register(createTool("tool1", "desc1"));
        registry.register(createTool("tool2", "desc2"));
        assertEquals(2, registry.size());

        registry.remove("tool1");
        assertEquals(1, registry.size());
        assertFalse(registry.contains("tool1"));
        assertTrue(registry.contains("tool2"));
    }

    @Test
    void testGetToolSpecs() {
        registry.register(createTool("a", "tool a"));
        registry.register(createTool("b", "tool b"));

        List<ToolSpec> specs = registry.getToolSpecs();
        assertEquals(2, specs.size());
    }

    @Test
    void testRegisterAllFromAnnotatedProvider() {
        Object provider = new AnnotatedProvider();
        registry.registerAll(provider);

        assertTrue(registry.contains("annotated_tool"));
        assertEquals(1, registry.size());
    }

    @Test
    void testOverwriteRegistration() {
        AgentTool tool1 = createTool("same", "first");
        AgentTool tool2 = createTool("same", "second");

        registry.register(tool1);
        registry.register(tool2);

        assertEquals(1, registry.size());
        assertEquals(tool2, registry.get("same"));
    }

    private AgentTool createTool(String name, String description) {
        return new AgentTool() {
            @Override
            public String getToolName() {
                return name;
            }

            @Override
            public ToolSpec getToolSpec() {
                return new ToolSpec(name, description, Map.of("type", "object"));
            }

            @Override
            public ToolResult invoke(ToolUse toolUse, ToolContext context) {
                return ToolResult.success(toolUse.getToolUseId(), "ok");
            }
        };
    }

    static class AnnotatedProvider {
        @Tool(name = "annotated_tool", description = "A test tool")
        public String doWork(@Param(value = "input", description = "Input text") String input) {
            return "result: " + input;
        }
    }
}
