package com.strands.agent;

import com.strands.model.Model;
import com.strands.model.ModelConfig;
import com.strands.model.StreamRequest;
import com.strands.types.streaming.StreamEvent;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AgentCancelTest {

    @Test
    void testCancelFlag() {
        Agent agent = Agent.builder()
                .model(new QuickModel())
                .build();

        assertFalse(agent.isCancelled());
        agent.cancel();
        assertTrue(agent.isCancelled());
    }

    @Test
    void testCancelResetOnInvoke() {
        Agent agent = Agent.builder()
                .model(new QuickModel())
                .build();

        agent.cancel();
        assertTrue(agent.isCancelled());

        agent.invoke("test");
        assertFalse(agent.isCancelled());
    }

    static class QuickModel implements Model {
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
