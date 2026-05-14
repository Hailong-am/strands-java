package com.strands.multiagent.a2a;

import com.strands.agent.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class A2AServer {

    private static final Logger log = LoggerFactory.getLogger(A2AServer.class);

    private final Map<String, A2AAgent> agents = new ConcurrentHashMap<>();
    private final String host;
    private final int port;

    public A2AServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void register(String name, Agent agent) {
        agents.put(name, new A2AAgent(agent, this));
        log.info("Registered A2A agent: {}", name);
    }

    public A2AAgent getAgent(String name) {
        return agents.get(name);
    }

    public Map<String, Object> getAgentCards() {
        Map<String, Object> cards = new ConcurrentHashMap<>();
        for (Map.Entry<String, A2AAgent> entry : agents.entrySet()) {
            cards.put(entry.getKey(), entry.getValue().getAgentCard());
        }
        return cards;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
