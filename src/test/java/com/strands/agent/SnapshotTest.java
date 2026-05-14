package com.strands.agent;

import com.strands.model.Model;
import com.strands.model.ModelConfig;
import com.strands.model.StreamRequest;
import com.strands.session.Snapshot;
import com.strands.types.Message;
import com.strands.types.streaming.StreamEvent;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SnapshotTest {

    @Test
    void testTakeAndLoadSnapshot() {
        Agent agent = buildAgent();
        agent.getMessages().add(Message.user("hello"));
        agent.getMessages().add(Message.assistant("hi there"));
        agent.getState().set("counter", 5);

        Snapshot snapshot = agent.takeSnapshot();

        assertNotNull(snapshot.getMessages());
        assertEquals(2, snapshot.getMessages().size());
        assertNotNull(snapshot.getState());
        assertEquals(5, snapshot.getState().get("counter"));
        assertNotNull(snapshot.getSystemPrompt());
    }

    @Test
    void testLoadSnapshotRestoresState() {
        Agent agent = buildAgent();
        agent.getMessages().add(Message.user("first"));
        agent.getState().set("key", "val");

        Snapshot snapshot = agent.takeSnapshot();

        agent.getMessages().clear();
        agent.getState().set("key", "changed");
        agent.getState().set("extra", "data");

        agent.loadSnapshot(snapshot);

        assertEquals(1, agent.getMessages().size());
        assertEquals("val", agent.getState().get("key"));
        assertFalse(agent.getState().has("extra"));
    }

    @Test
    void testTakeSnapshotWithPreset() {
        Agent agent = buildAgent();
        agent.getMessages().add(Message.user("test"));
        agent.getState().set("x", 1);

        Snapshot snapshot = agent.takeSnapshot("session", null, null);

        assertNotNull(snapshot.getMessages());
        assertNotNull(snapshot.getState());
        assertNotNull(snapshot.getSystemPrompt());
    }

    @Test
    void testTakeSnapshotWithInclude() {
        Agent agent = buildAgent();
        agent.getMessages().add(Message.user("test"));
        agent.getState().set("x", 1);

        Snapshot snapshot = agent.takeSnapshot(null, Set.of("messages"), null);

        assertNotNull(snapshot.getMessages());
        assertNull(snapshot.getState());
        assertNull(snapshot.getSystemPrompt());
    }

    @Test
    void testTakeSnapshotWithExclude() {
        Agent agent = buildAgent();
        agent.getMessages().add(Message.user("test"));
        agent.getState().set("x", 1);
        agent.setSystemPrompt("You are a test bot");

        Snapshot snapshot = agent.takeSnapshot(null, null, Set.of("systemPrompt"));

        assertNotNull(snapshot.getMessages());
        assertNotNull(snapshot.getState());
        assertNull(snapshot.getSystemPrompt());
    }

    @Test
    void testSnapshotWithAppData() {
        Agent agent = buildAgent();
        agent.getMessages().add(Message.user("test"));

        Snapshot snapshot = agent.takeSnapshot()
                .withAppData(Map.of("version", "1.2", "env", "test"));

        assertNotNull(snapshot.getAppData());
        assertEquals("1.2", snapshot.getAppData().get("version"));
    }

    private Agent buildAgent() {
        return Agent.builder()
                .model(new MockModel())
                .systemPrompt("test system prompt")
                .build();
    }

    static class MockModel implements Model {
        @Override
        public Iterator<StreamEvent> stream(StreamRequest request) {
            return List.of(
                    StreamEvent.messageStart("assistant"),
                    StreamEvent.contentBlockDelta(0, Map.of("text", "response")),
                    StreamEvent.messageStop("end_turn"),
                    StreamEvent.metadata(1, 1, 10)
            ).iterator();
        }

        @Override
        public ModelConfig getConfig() {
            return new ModelConfig("mock");
        }

        @Override
        public void updateConfig(Map<String, Object> cfg) {
        }
    }
}
