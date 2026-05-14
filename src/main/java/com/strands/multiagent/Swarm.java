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

public class Swarm implements MultiAgent {

    private static final Logger log = LoggerFactory.getLogger(Swarm.class);
    private static final int MAX_HANDOFFS = 50;

    private final Map<String, Agent> agents;
    private final String startAgent;

    public Swarm(Map<String, Agent> agents, String startAgent) {
        this.agents = agents;
        this.startAgent = startAgent;
        registerHandoffTools();
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
            lastResult = current.invoke(prompt);

            String handoffTarget = current.getState().get("_handoff_target", String.class);
            if (handoffTarget != null) {
                current.getState().remove("_handoff_target");
                String handoffPrompt = current.getState().get("_handoff_prompt", String.class);
                current.getState().remove("_handoff_prompt");
                currentAgentName = handoffTarget;
                prompt = handoffPrompt != null ? handoffPrompt : lastResult.toString();
                handoffs++;
            } else {
                break;
            }
        }

        return lastResult;
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
