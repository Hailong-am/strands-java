package com.strands.agent;

import com.strands.model.Model;
import com.strands.model.ModelConfig;
import com.strands.model.StreamRequest;
import com.strands.tool.AgentTool;
import com.strands.tool.ToolContext;
import com.strands.types.*;
import com.strands.types.streaming.StreamEvent;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AgentTest {

    @Test
    void testSimpleInvocation() {
        Model mockModel = new MockModel("Hello, world!");

        Agent agent = Agent.builder()
                .model(mockModel)
                .systemPrompt("You are helpful")
                .build();

        AgentResult result = agent.invoke("Hi");

        assertEquals(StopReason.END_TURN, result.getStopReason());
        assertEquals("Hello, world!", result.toString());
        assertEquals(2, agent.getMessages().size()); // user + assistant
    }

    @Test
    void testToolInvocation() {
        AgentTool calculator = new AgentTool() {
            @Override
            public String getToolName() {
                return "add";
            }

            @Override
            public ToolSpec getToolSpec() {
                return new ToolSpec("add", "Adds two numbers", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "a", Map.of("type", "integer"),
                                "b", Map.of("type", "integer")
                        ),
                        "required", List.of("a", "b")
                ));
            }

            @Override
            public ToolResult invoke(ToolUse toolUse, ToolContext context) {
                int a = ((Number) toolUse.getInput().get("a")).intValue();
                int b = ((Number) toolUse.getInput().get("b")).intValue();
                return ToolResult.success(toolUse.getToolUseId(), String.valueOf(a + b));
            }
        };

        Model toolModel = new ToolCallThenAnswerModel();

        Agent agent = Agent.builder()
                .model(toolModel)
                .systemPrompt("Use tools when needed")
                .tools(calculator)
                .build();

        AgentResult result = agent.invoke("What is 2 + 3?");

        assertEquals(StopReason.END_TURN, result.getStopReason());
        assertEquals("The answer is 5", result.toString());
    }

    static class MockModel implements Model {
        private final String response;
        private final ModelConfig config = new ModelConfig("mock");

        MockModel(String response) {
            this.response = response;
        }

        @Override
        public Iterator<StreamEvent> stream(StreamRequest request) {
            List<StreamEvent> events = List.of(
                    StreamEvent.messageStart("assistant"),
                    StreamEvent.contentBlockStart(0, Map.of()),
                    StreamEvent.contentBlockDelta(0, Map.of("text", response)),
                    StreamEvent.contentBlockStop(0),
                    StreamEvent.messageStop("end_turn"),
                    StreamEvent.metadata(10, 5, 100)
            );
            return events.iterator();
        }

        @Override
        public ModelConfig getConfig() {
            return config;
        }

        @Override
        public void updateConfig(Map<String, Object> cfg) {
        }
    }

    static class ToolCallThenAnswerModel implements Model {
        private int callCount = 0;
        private final ModelConfig config = new ModelConfig("mock-tool");

        @Override
        public Iterator<StreamEvent> stream(StreamRequest request) {
            callCount++;
            if (callCount == 1) {
                String toolUseId = "tool-123";
                List<StreamEvent> events = List.of(
                        StreamEvent.messageStart("assistant"),
                        StreamEvent.contentBlockStart(0, Map.of("toolUse", Map.of(
                                "toolUseId", toolUseId,
                                "name", "add"
                        ))),
                        StreamEvent.contentBlockDelta(0, Map.of("toolUse", Map.of("input", "{\"a\":2,\"b\":3}"))),
                        StreamEvent.contentBlockStop(0),
                        StreamEvent.messageStop("tool_use"),
                        StreamEvent.metadata(10, 5, 50)
                );
                return events.iterator();
            } else {
                List<StreamEvent> events = List.of(
                        StreamEvent.messageStart("assistant"),
                        StreamEvent.contentBlockStart(0, Map.of()),
                        StreamEvent.contentBlockDelta(0, Map.of("text", "The answer is 5")),
                        StreamEvent.contentBlockStop(0),
                        StreamEvent.messageStop("end_turn"),
                        StreamEvent.metadata(15, 8, 80)
                );
                return events.iterator();
            }
        }

        @Override
        public ModelConfig getConfig() {
            return config;
        }

        @Override
        public void updateConfig(Map<String, Object> cfg) {
        }
    }
}
