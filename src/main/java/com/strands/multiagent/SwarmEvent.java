package com.strands.multiagent;

import java.time.Instant;

/**
 * Events emitted by Swarm during execution for observability.
 */
public class SwarmEvent {

    public enum Type {
        HANDOFF, AGENT_START, AGENT_STOP
    }

    private final Type type;
    private final String fromAgent;
    private final String toAgent;
    private final Instant timestamp;

    private SwarmEvent(Type type, String fromAgent, String toAgent) {
        this.type = type;
        this.fromAgent = fromAgent;
        this.toAgent = toAgent;
        this.timestamp = Instant.now();
    }

    public static SwarmEvent handoff(String from, String to) {
        return new SwarmEvent(Type.HANDOFF, from, to);
    }

    public static SwarmEvent agentStart(String agentName) {
        return new SwarmEvent(Type.AGENT_START, agentName, null);
    }

    public static SwarmEvent agentStop(String agentName) {
        return new SwarmEvent(Type.AGENT_STOP, agentName, null);
    }

    public Type getType() { return type; }
    public String getFromAgent() { return fromAgent; }
    public String getToAgent() { return toAgent; }
    public Instant getTimestamp() { return timestamp; }
}
