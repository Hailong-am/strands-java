package com.strands.agent;

import com.strands.model.Model;
import com.strands.model.ModelConfig;
import com.strands.model.StreamRequest;
import com.strands.types.Message;
import com.strands.types.StopReason;
import com.strands.types.streaming.StreamEvent;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SlidingWindowConversationManagerTest {

    @Test
    void testNoTrimWhenUnderWindowSize() {
        Agent agent = buildAgent(40);
        for (int i = 0; i < 10; i++) {
            agent.getMessages().add(Message.user("msg " + i));
        }

        new SlidingWindowConversationManager(40).applyManagement(agent);
        assertEquals(10, agent.getMessages().size());
    }

    @Test
    void testTrimsWhenOverWindowSize() {
        Agent agent = buildAgent(5);
        for (int i = 0; i < 10; i++) {
            agent.getMessages().add(Message.user("msg " + i));
        }

        new SlidingWindowConversationManager(5).applyManagement(agent);
        assertEquals(5, agent.getMessages().size());
        assertEquals("msg 5", agent.getMessages().get(0).getTextContent());
    }

    @Test
    void testReduceContextRemovesQuarter() {
        Agent agent = buildAgent(100);
        for (int i = 0; i < 20; i++) {
            agent.getMessages().add(Message.user("msg " + i));
        }

        new SlidingWindowConversationManager(100).reduceContext(agent, new RuntimeException("overflow"));
        assertTrue(agent.getMessages().size() < 20);
    }

    private Agent buildAgent(int windowSize) {
        return Agent.builder()
                .model(new NoOpModel())
                .conversationManager(new SlidingWindowConversationManager(windowSize))
                .build();
    }

    static class NoOpModel implements Model {
        @Override
        public Iterator<StreamEvent> stream(StreamRequest request) {
            return List.of(
                    StreamEvent.messageStart("assistant"),
                    StreamEvent.contentBlockDelta(0, Map.of("text", "ok")),
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
