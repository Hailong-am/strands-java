package com.strands.types;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    void testUserMessage() {
        Message msg = Message.user("hello");
        assertEquals(Message.Role.USER, msg.getRole());
        assertEquals("hello", msg.getTextContent());
        assertEquals(1, msg.getContent().size());
    }

    @Test
    void testAssistantMessage() {
        Message msg = Message.assistant("hi there");
        assertEquals(Message.Role.ASSISTANT, msg.getRole());
        assertEquals("hi there", msg.getTextContent());
    }

    @Test
    void testToolResultMessage() {
        ToolResult r1 = ToolResult.success("t1", "result1");
        ToolResult r2 = ToolResult.success("t2", "result2");
        Message msg = Message.toolResult(r1, r2);

        assertEquals(Message.Role.USER, msg.getRole());
        assertEquals(2, msg.getContent().size());
        assertTrue(msg.getContent().get(0).isToolResult());
        assertTrue(msg.getContent().get(1).isToolResult());
    }

    @Test
    void testGetTextContentMultipleBlocks() {
        Message msg = new Message(Message.Role.ASSISTANT, List.of(
                ContentBlock.fromText("Hello "),
                ContentBlock.fromText("World")
        ));
        assertEquals("Hello World", msg.getTextContent());
    }

    @Test
    void testGetToolUses() {
        ToolUse tu1 = new ToolUse();
        tu1.setToolUseId("id1");
        tu1.setName("tool1");
        tu1.setInput(Map.of());

        ToolUse tu2 = new ToolUse();
        tu2.setToolUseId("id2");
        tu2.setName("tool2");
        tu2.setInput(Map.of());

        Message msg = new Message(Message.Role.ASSISTANT, List.of(
                ContentBlock.fromText("Let me use tools"),
                ContentBlock.fromToolUse(tu1),
                ContentBlock.fromToolUse(tu2)
        ));

        List<ToolUse> uses = msg.getToolUses();
        assertEquals(2, uses.size());
        assertEquals("tool1", uses.get(0).getName());
        assertEquals("tool2", uses.get(1).getName());
    }

    @Test
    void testHasToolUse() {
        Message withTool = new Message(Message.Role.ASSISTANT, List.of(
                ContentBlock.fromToolUse(createToolUse("t1", "search"))
        ));
        assertTrue(withTool.hasToolUse());

        Message withoutTool = Message.assistant("no tools");
        assertFalse(withoutTool.hasToolUse());
    }

    @Test
    void testEmptyMessage() {
        Message msg = new Message();
        assertNotNull(msg.getContent());
        assertTrue(msg.getContent().isEmpty());
        assertEquals("", msg.getTextContent());
    }

    private ToolUse createToolUse(String id, String name) {
        ToolUse tu = new ToolUse();
        tu.setToolUseId(id);
        tu.setName(name);
        tu.setInput(Map.of());
        return tu;
    }
}
