package com.strands.multiagent.a2a;

import com.strands.agent.AgentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class A2AExecutor {

    private static final Logger log = LoggerFactory.getLogger(A2AExecutor.class);

    private final A2AServer server;

    public A2AExecutor(A2AServer server) {
        this.server = server;
    }

    public AgentResult execute(String agentName, String prompt) {
        A2AAgent agent = server.getAgent(agentName);
        if (agent == null) {
            throw new IllegalArgumentException("A2A agent not found: " + agentName);
        }
        return agent.invoke(prompt);
    }

    public CompletableFuture<AgentResult> executeAsync(String agentName, String prompt) {
        return CompletableFuture.supplyAsync(() -> execute(agentName, prompt));
    }

    public Map<String, Object> discoverAgents() {
        return server.getAgentCards();
    }
}
