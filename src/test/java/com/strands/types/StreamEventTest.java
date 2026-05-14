package com.strands.types;

import com.strands.types.streaming.StreamEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StreamEventTest {

    @Test
    void testMessageStart() {
        StreamEvent event = StreamEvent.messageStart("assistant");
        assertEquals(StreamEvent.Type.MESSAGE_START, event.getType());
        assertEquals("assistant", event.getData().get("role"));
    }

    @Test
    void testContentBlockStart() {
        StreamEvent event = StreamEvent.contentBlockStart(0, Map.of("toolUse", Map.of(
                "toolUseId", "t1", "name", "calc")));
        assertEquals(StreamEvent.Type.CONTENT_BLOCK_START, event.getType());
        assertEquals(0, event.getData().get("contentBlockIndex"));
    }

    @Test
    void testContentBlockDeltaText() {
        StreamEvent event = StreamEvent.contentBlockDelta(0, Map.of("text", "Hello"));
        assertEquals(StreamEvent.Type.CONTENT_BLOCK_DELTA, event.getType());
        assertEquals(0, event.getData().get("contentBlockIndex"));
    }

    @Test
    void testContentBlockStop() {
        StreamEvent event = StreamEvent.contentBlockStop(0);
        assertEquals(StreamEvent.Type.CONTENT_BLOCK_STOP, event.getType());
        assertEquals(0, event.getData().get("contentBlockIndex"));
    }

    @Test
    void testMessageStop() {
        StreamEvent event = StreamEvent.messageStop("end_turn");
        assertEquals(StreamEvent.Type.MESSAGE_STOP, event.getType());
        assertEquals("end_turn", event.getData().get("stopReason"));
    }

    @Test
    void testMetadata() {
        StreamEvent event = StreamEvent.metadata(100, 50, 200);
        assertEquals(StreamEvent.Type.METADATA, event.getType());
    }
}
