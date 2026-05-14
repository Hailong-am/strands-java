package com.strands.model;

import com.strands.types.*;
import com.strands.types.streaming.StreamEvent;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StreamProcessorTest {

    private final StreamProcessor processor = new StreamProcessor();

    @Test
    void testBasicTextStream() {
        List<StreamEvent> events = List.of(
                StreamEvent.messageStart("assistant"),
                StreamEvent.contentBlockStart(0, Map.of()),
                StreamEvent.contentBlockDelta(0, Map.of("text", "Hello")),
                StreamEvent.contentBlockDelta(0, Map.of("text", " world")),
                StreamEvent.contentBlockStop(0),
                StreamEvent.messageStop("end_turn"),
                StreamEvent.metadata(10, 5, 100)
        );

        StreamResult result = processor.process(events.iterator(), null);

        assertEquals(StopReason.END_TURN, result.getStopReason());
        assertEquals("Hello world", result.getMessage().getTextContent());
        assertEquals(10, result.getUsage().getInputTokens());
        assertEquals(5, result.getUsage().getOutputTokens());
    }

    @Test
    void testToolUseStream() {
        List<StreamEvent> events = List.of(
                StreamEvent.messageStart("assistant"),
                StreamEvent.contentBlockStart(0, Map.of("toolUse", Map.of(
                        "toolUseId", "tool-1",
                        "name", "calc"
                ))),
                StreamEvent.contentBlockDelta(0, Map.of("toolUse", Map.of("input", "{\"a\":1}"))),
                StreamEvent.contentBlockStop(0),
                StreamEvent.messageStop("tool_use"),
                StreamEvent.metadata(10, 5, 50)
        );

        StreamResult result = processor.process(events.iterator(), null);

        assertEquals(StopReason.TOOL_USE, result.getStopReason());
        List<ToolUse> toolUses = result.getMessage().getToolUses();
        assertEquals(1, toolUses.size());
        assertEquals("calc", toolUses.get(0).getName());
        assertEquals("tool-1", toolUses.get(0).getToolUseId());
        assertEquals(1, toolUses.get(0).getInput().get("a"));
    }

    @Test
    void testTextWithoutContentBlockStart() {
        // Bedrock sometimes skips contentBlockStart for text
        List<StreamEvent> events = List.of(
                StreamEvent.messageStart("assistant"),
                StreamEvent.contentBlockDelta(0, Map.of("text", "No start event")),
                StreamEvent.messageStop("end_turn"),
                StreamEvent.metadata(5, 3, 50)
        );

        StreamResult result = processor.process(events.iterator(), null);

        assertEquals("No start event", result.getMessage().getTextContent());
    }

    @Test
    void testMultipleContentBlocks() {
        List<StreamEvent> events = List.of(
                StreamEvent.messageStart("assistant"),
                StreamEvent.contentBlockStart(0, Map.of()),
                StreamEvent.contentBlockDelta(0, Map.of("text", "First")),
                StreamEvent.contentBlockStop(0),
                StreamEvent.contentBlockStart(1, Map.of("toolUse", Map.of(
                        "toolUseId", "t1",
                        "name", "search"
                ))),
                StreamEvent.contentBlockDelta(1, Map.of("toolUse", Map.of("input", "{}"))),
                StreamEvent.contentBlockStop(1),
                StreamEvent.messageStop("tool_use"),
                StreamEvent.metadata(20, 10, 100)
        );

        StreamResult result = processor.process(events.iterator(), null);

        assertEquals(StopReason.TOOL_USE, result.getStopReason());
        assertEquals(2, result.getMessage().getContent().size());
        assertTrue(result.getMessage().getContent().get(0).isText());
        assertTrue(result.getMessage().getContent().get(1).isToolUse());
    }

    @Test
    void testStreamHandlerCallbacks() {
        List<String> textDeltas = new ArrayList<>();
        List<String> toolStarts = new ArrayList<>();

        StreamHandler handler = new StreamHandler() {
            @Override
            public void onTextDelta(String delta) {
                textDeltas.add(delta);
            }

            @Override
            public void onToolUseStart(String toolName, String toolUseId) {
                toolStarts.add(toolName);
            }

            @Override
            public void onToolUseDelta(String delta) {
            }

            @Override
            public void onComplete(Message message, StopReason stopReason) {
            }
        };

        List<StreamEvent> events = List.of(
                StreamEvent.messageStart("assistant"),
                StreamEvent.contentBlockStart(0, Map.of()),
                StreamEvent.contentBlockDelta(0, Map.of("text", "Hi")),
                StreamEvent.contentBlockDelta(0, Map.of("text", " there")),
                StreamEvent.contentBlockStop(0),
                StreamEvent.messageStop("end_turn"),
                StreamEvent.metadata(5, 3, 50)
        );

        processor.process(events.iterator(), handler);

        assertEquals(List.of("Hi", " there"), textDeltas);
        assertTrue(toolStarts.isEmpty());
    }

    @Test
    void testEmptyTextBlockNotAdded() {
        List<StreamEvent> events = List.of(
                StreamEvent.messageStart("assistant"),
                StreamEvent.contentBlockStart(0, Map.of()),
                StreamEvent.contentBlockStop(0),
                StreamEvent.messageStop("end_turn"),
                StreamEvent.metadata(5, 3, 50)
        );

        StreamResult result = processor.process(events.iterator(), null);

        assertEquals(0, result.getMessage().getContent().size());
    }
}
