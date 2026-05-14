package com.strands.multiagent.a2a;

import com.strands.agent.Agent;
import com.strands.agent.AgentResult;
import com.strands.multiagent.MultiAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class A2AAgent implements MultiAgent {

    private static final Logger log = LoggerFactory.getLogger(A2AAgent.class);

    private final Agent agent;
    private final A2AServer server;

    public A2AAgent(Agent agent, A2AServer server) {
        this.agent = agent;
        this.server = server;
    }

    @Override
    public AgentResult invoke(String prompt) {
        return agent.invoke(prompt);
    }

    public Map<String, Object> getAgentCard() {
        return Map.of(
                "name", agent.getName() != null ? agent.getName() : agent.getAgentId(),
                "description", agent.getSystemPrompt() != null ? agent.getSystemPrompt() : "",
                "capabilities", Map.of(
                        "tools", agent.getToolRegistry().getToolSpecs()
                )
        );
    }

    public Agent getAgent() {
        return agent;
    }

    public A2AServer getServer() {
        return server;
    }
}
