package com.strands.agent;

import com.strands.model.Model;
import com.strands.model.ModelConfig;
import com.strands.model.StreamRequest;
import com.strands.types.exceptions.ConcurrencyException;
import com.strands.types.streaming.StreamEvent;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentInvocationTest {

    @Test
    void testThrowModeRejectsConcurrentCalls() throws InterruptedException {
        CountDownLatch modelEnteredLatch = new CountDownLatch(1);
        CountDownLatch modelReleaseLatch = new CountDownLatch(1);

        Model slowModel = new Model() {
            @Override
            public Iterator<StreamEvent> stream(StreamRequest request) {
                modelEnteredLatch.countDown();
                try {
                    modelReleaseLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return List.of(
                        StreamEvent.messageStart("assistant"),
                        StreamEvent.contentBlockDelta(0, Map.of("text", "done")),
                        StreamEvent.messageStop("end_turn"),
                        StreamEvent.metadata(1, 1, 10)
                ).iterator();
            }

            @Override
            public ModelConfig getConfig() {
                return new ModelConfig("slow");
            }

            @Override
            public void updateConfig(Map<String, Object> cfg) {
            }
        };

        Agent agent = Agent.builder()
                .model(slowModel)
                .concurrentInvocationMode(ConcurrentInvocationMode.THROW)
                .build();

        AtomicReference<Exception> secondCallException = new AtomicReference<>();

        Thread first = new Thread(() -> agent.invoke("first"));
        first.start();

        modelEnteredLatch.await();

        Thread second = new Thread(() -> {
            try {
                agent.invoke("second");
            } catch (Exception e) {
                secondCallException.set(e);
            }
        });
        second.start();
        second.join(2000);

        modelReleaseLatch.countDown();
        first.join(2000);

        assertNotNull(secondCallException.get());
        assertInstanceOf(ConcurrencyException.class, secondCallException.get());
    }

    @Test
    void testUnsafeReentrantAllowsConcurrent() throws InterruptedException {
        Model quickModel = new Model() {
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
                return new ModelConfig("quick");
            }

            @Override
            public void updateConfig(Map<String, Object> cfg) {
            }
        };

        Agent agent = Agent.builder()
                .model(quickModel)
                .concurrentInvocationMode(ConcurrentInvocationMode.UNSAFE_REENTRANT)
                .build();

        AgentResult result = agent.invoke("test");
        assertNotNull(result);
        assertEquals("ok", result.toString());
    }
}
