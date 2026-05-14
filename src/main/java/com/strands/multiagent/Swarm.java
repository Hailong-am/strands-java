package com.strands.multiagent;

import com.strands.agent.Agent;
import com.strands.agent.AgentResult;
import com.strands.agent.AgentState;
import com.strands.tool.AgentTool;
import com.strands.tool.ToolContext;
import com.strands.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

public class Swarm implements MultiAgent {

    private static final Logger log = LoggerFactory.getLogger(Swarm.class);
    private static final int MAX_HANDOFFS = 50;

    private final Map<String, Agent> agents;
    private final String startAgent;
    private final SharedMemory sharedMemory = new SharedMemory();
    private final List<Consumer<SwarmEvent>> eventListeners = new ArrayList<>();

    public Swarm(Map<String, Agent> agents, String startAgent) {
        this.agents = agents;
        this.startAgent = startAgent;
        registerHandoffTools();
        registerCoordinationTools();
    }

    public SharedMemory getSharedMemory() {
        return sharedMemory;
    }

    public void addEventListener(Consumer<SwarmEvent> listener) {
        eventListeners.add(listener);
    }

    @Override
    public AgentResult invoke(String prompt) {
        String currentAgentName = startAgent;
        int handoffs = 0;
        AgentResult lastResult = null;

        while (handoffs < MAX_HANDOFFS) {
            Agent current = agents.get(currentAgentName);
            if (current == null) {
                throw new IllegalStateException("Agent not found: " + currentAgentName);
            }

            log.debug("Swarm invoking agent: {}", currentAgentName);
            emitEvent(SwarmEvent.agentStart(currentAgentName));
            lastResult = current.invoke(prompt);
            emitEvent(SwarmEvent.agentStop(currentAgentName));

            String handoffTarget = current.getState().get("_handoff_target", String.class);
            if (handoffTarget != null) {
                current.getState().remove("_handoff_target");
                String handoffPrompt = current.getState().get("_handoff_prompt", String.class);
                current.getState().remove("_handoff_prompt");
                emitEvent(SwarmEvent.handoff(currentAgentName, handoffTarget));
                currentAgentName = handoffTarget;
                prompt = handoffPrompt != null ? handoffPrompt : lastResult.toString();
                handoffs++;
            } else {
                break;
            }
        }

        return lastResult;
    }

    private void emitEvent(SwarmEvent event) {
        for (Consumer<SwarmEvent> listener : eventListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                log.warn("Swarm event listener error", e);
            }
        }
    }

    private void registerHandoffTools() {
        for (Map.Entry<String, Agent> entry : agents.entrySet()) {
            String agentName = entry.getKey();
            Agent agent = entry.getValue();

            for (Map.Entry<String, Agent> target : agents.entrySet()) {
                if (!target.getKey().equals(agentName)) {
                    HandoffTool handoffTool = new HandoffTool(target.getKey());
                    agent.getToolRegistry().register(handoffTool);
                }
            }
        }
    }

    private void registerCoordinationTools() {
        AgentTool readMemory = new AgentTool() {
            @Override
            public String getToolName() { return "read_shared_memory"; }

            @Override
            public ToolSpec getToolSpec() {
                return new ToolSpec(getToolName(), "Read a value from shared working memory",
                        Map.of("type", "object",
                                "properties", Map.of("key", Map.of("type", "string", "description", "Key to read")),
                                "required", List.of("key")));
            }

            @Override
            public ToolResult invoke(ToolUse toolUse, ToolContext context) {
                String key = (String) toolUse.getInput().get("key");
                Object value = sharedMemory.get(key);
                return ToolResult.success(toolUse.getToolUseId(),
                        value != null ? value.toString() : "null");
            }
        };

        AgentTool writeMemory = new AgentTool() {
            @Override
            public String getToolName() { return "write_shared_memory"; }

            @Override
            public ToolSpec getToolSpec() {
                return new ToolSpec(getToolName(), "Write a value to shared working memory",
                        Map.of("type", "object",
                                "properties", Map.of(
                                        "key", Map.of("type", "string", "description", "Key to write"),
                                        "value", Map.of("type", "string", "description", "Value to store")),
                                "required", List.of("key", "value")));
            }

            @Override
            public ToolResult invoke(ToolUse toolUse, ToolContext context) {
                String key = (String) toolUse.getInput().get("key");
                String value = (String) toolUse.getInput().get("value");
                sharedMemory.put(key, value);
                return ToolResult.success(toolUse.getToolUseId(), "Stored: " + key);
            }
        };

        for (Agent agent : agents.values()) {
            agent.getToolRegistry().register(readMemory);
            agent.getToolRegistry().register(writeMemory);
        }
    }

    public Map<String, Object> serializeState() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("sharedMemory", new LinkedHashMap<>(sharedMemory.getAll()));
        Map<String, Map<String, Object>> agentStates = new LinkedHashMap<>();
        for (Map.Entry<String, Agent> entry : agents.entrySet()) {
            agentStates.put(entry.getKey(), entry.getValue().getState().toMap());
        }
        state.put("agentStates", agentStates);
        return state;
    }

    @SuppressWarnings("unchecked")
    public void deserializeState(Map<String, Object> state) {
        if (state == null) return;

        Object memoryData = state.get("sharedMemory");
        if (memoryData instanceof Map) {
            sharedMemory.clear();
            ((Map<String, Object>) memoryData).forEach(sharedMemory::put);
        }

        Object agentStatesData = state.get("agentStates");
        if (agentStatesData instanceof Map) {
            Map<String, Map<String, Object>> agentStates = (Map<String, Map<String, Object>>) agentStatesData;
            for (Map.Entry<String, Map<String, Object>> entry : agentStates.entrySet()) {
                Agent agent = agents.get(entry.getKey());
                if (agent != null) {
                    entry.getValue().forEach((k, v) -> agent.getState().set(k, v));
                }
            }
        }
    }

    public static class SharedMemory {
        private final Map<String, Object> data = new java.util.concurrent.ConcurrentHashMap<>();

        public void put(String key, Object value) { data.put(key, value); }
        public Object get(String key) { return data.get(key); }
        public void remove(String key) { data.remove(key); }
        public Map<String, Object> getAll() { return Collections.unmodifiableMap(data); }
        public void clear() { data.clear(); }
    }

    private static class HandoffTool implements AgentTool {
        private final String targetAgent;

        HandoffTool(String targetAgent) {
            this.targetAgent = targetAgent;
        }

        @Override
        public String getToolName() {
            return "handoff_to_" + targetAgent;
        }

        @Override
        public ToolSpec getToolSpec() {
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("prompt", Map.of(
                    "type", "string",
                    "description", "The prompt/context to pass to the target agent"
            ));

            Map<String, Object> inputSchema = new LinkedHashMap<>();
            inputSchema.put("type", "object");
            inputSchema.put("properties", properties);
            inputSchema.put("required", List.of("prompt"));

            return new ToolSpec(getToolName(),
                    "Hand off the conversation to " + targetAgent,
                    inputSchema);
        }

        @Override
        public ToolResult invoke(ToolUse toolUse, ToolContext context) {
            String prompt = (String) toolUse.getInput().get("prompt");
            context.setState("_handoff_target", targetAgent);
            context.setState("_handoff_prompt", prompt);
            return ToolResult.success(toolUse.getToolUseId(), "Handing off to " + targetAgent);
        }
    }
}
