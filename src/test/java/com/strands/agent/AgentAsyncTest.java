package com.strands.agent;

import com.strands.model.Model;
import com.strands.model.ModelConfig;
import com.strands.model.StreamRequest;
import com.strands.types.StopReason;
import com.strands.types.streaming.StreamEvent;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class AgentAsyncTest {

    @Test
    void testInvokeAsync() throws ExecutionException, InterruptedException, TimeoutException {
        Agent agent = Agent.builder()
                .model(new MockModel("async result"))
                .concurrentInvocationMode(ConcurrentInvocationMode.UNSAFE_REENTRANT)
                .build();

        CompletableFuture<AgentResult> future = agent.invokeAsync("test");
        AgentResult result = future.get(5, TimeUnit.SECONDS);

        assertNotNull(result);
        assertEquals(StopReason.END_TURN, result.getStopReason());
        assertEquals("async result", result.toString());
    }

    @Test
    void testMultipleAsyncInvocations() throws ExecutionException, InterruptedException, TimeoutException {
        Agent agent = Agent.builder()
                .model(new MockModel("response"))
                .concurrentInvocationMode(ConcurrentInvocationMode.UNSAFE_REENTRANT)
                .build();

        CompletableFuture<AgentResult> f1 = agent.invokeAsync("first");
        CompletableFuture<AgentResult> f2 = agent.invokeAsync("second");

        AgentResult r1 = f1.get(5, TimeUnit.SECONDS);
        AgentResult r2 = f2.get(5, TimeUnit.SECONDS);

        assertNotNull(r1);
        assertNotNull(r2);
    }

    static class MockModel implements Model {
        private final String response;

        MockModel(String response) {
            this.response = response;
        }

        @Override
        public Iterator<StreamEvent> stream(StreamRequest request) {
            return List.of(
                    StreamEvent.messageStart("assistant"),
                    StreamEvent.contentBlockStart(0, Map.of()),
                    StreamEvent.contentBlockDelta(0, Map.of("text", response)),
                    StreamEvent.contentBlockStop(0),
                    StreamEvent.messageStop("end_turn"),
                    StreamEvent.metadata(10, 5, 50)
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
