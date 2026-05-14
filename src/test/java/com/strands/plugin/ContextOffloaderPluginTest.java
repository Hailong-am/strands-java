package com.strands.plugin;

import com.strands.hook.events.AfterToolCallEvent;
import com.strands.plugin.offloader.ContextOffloaderPlugin;
import com.strands.plugin.offloader.InMemoryStorage;
import com.strands.types.ToolResult;
import com.strands.types.ToolResultContent;
import com.strands.types.ToolUse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ContextOffloaderPluginTest {

    private InMemoryStorage storage;
    private ContextOffloaderPlugin plugin;

    @BeforeEach
    void setUp() {
        storage = new InMemoryStorage();
        plugin = new ContextOffloaderPlugin(storage, 100, 20);
    }

    @Test
    void testShortResultNotTruncated() {
        ToolUse toolUse = new ToolUse();
        toolUse.setToolUseId("t1");
        toolUse.setName("test");
        toolUse.setInput(Map.of());

        ToolResult result = ToolResult.success("t1", "Short result");
        AfterToolCallEvent event = new AfterToolCallEvent(toolUse, result);

        plugin.onAfterToolCall(event);

        assertEquals("Short result", event.getResult().getContent().get(0).getText());
    }

    @Test
    void testLongResultTruncated() {
        ToolUse toolUse = new ToolUse();
        toolUse.setToolUseId("t1");
        toolUse.setName("test");
        toolUse.setInput(Map.of());

        String longContent = "x".repeat(200);
        ToolResult result = ToolResult.success("t1", longContent);
        AfterToolCallEvent event = new AfterToolCallEvent(toolUse, result);

        plugin.onAfterToolCall(event);

        String truncatedText = event.getResult().getContent().get(0).getText();
        assertTrue(truncatedText.contains("[Content truncated."));
        assertTrue(truncatedText.contains("200 chars total"));
        assertTrue(truncatedText.contains("retrieve_offloaded"));
        assertTrue(truncatedText.startsWith("xxxxxxxxxxxxxxxxxxxx")); // 20 char preview
    }

    @Test
    void testRetrieveOffloadedContent() {
        ToolUse toolUse = new ToolUse();
        toolUse.setToolUseId("t1");
        toolUse.setName("test");
        toolUse.setInput(Map.of());

        String longContent = "Important data: " + "y".repeat(200);
        ToolResult result = ToolResult.success("t1", longContent);
        AfterToolCallEvent event = new AfterToolCallEvent(toolUse, result);

        plugin.onAfterToolCall(event);

        String truncatedText = event.getResult().getContent().get(0).getText();
        String key = extractKey(truncatedText);

        String retrieved = plugin.retrieveOffloaded(key);
        assertEquals(longContent, retrieved);
    }

    @Test
    void testRetrieveNonExistentKey() {
        String result = plugin.retrieveOffloaded("nonexistent_key");
        assertTrue(result.contains("Content not found"));
    }

    @Test
    void testNullResultIgnored() {
        ToolUse toolUse = new ToolUse();
        toolUse.setToolUseId("t1");
        toolUse.setName("test");
        toolUse.setInput(Map.of());

        AfterToolCallEvent event = new AfterToolCallEvent(toolUse, null);
        assertDoesNotThrow(() -> plugin.onAfterToolCall(event));
    }

    @Test
    void testDiscoverTools() {
        List<?> tools = plugin.discoverTools();
        assertEquals(1, tools.size());
    }

    private String extractKey(String text) {
        int start = text.indexOf("key='") + 5;
        int end = text.indexOf("'", start);
        return text.substring(start, end);
    }
}
