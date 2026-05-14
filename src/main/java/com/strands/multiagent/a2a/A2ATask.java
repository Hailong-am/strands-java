package com.strands.multiagent.a2a;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a long-running A2A task. Tasks track the execution state
 * of an agent request across its lifecycle.
 */
public class A2ATask {

    public enum Status {
        SUBMITTED, WORKING, INPUT_REQUIRED, COMPLETED, FAILED, CANCELLED
    }

    private final String id;
    private final String agentName;
    private Status status;
    private String input;
    private String output;
    private List<Map<String, Object>> artifacts;
    private final Instant createdAt;
    private Instant updatedAt;

    public A2ATask(String agentName, String input) {
        this.id = UUID.randomUUID().toString();
        this.agentName = agentName;
        this.input = input;
        this.status = Status.SUBMITTED;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }
    public String getAgentName() { return agentName; }
    public Status getStatus() { return status; }
    public String getInput() { return input; }
    public String getOutput() { return output; }
    public List<Map<String, Object>> getArtifacts() { return artifacts; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setStatus(Status status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void setOutput(String output) {
        this.output = output;
        this.updatedAt = Instant.now();
    }

    public void setArtifacts(List<Map<String, Object>> artifacts) {
        this.artifacts = artifacts;
        this.updatedAt = Instant.now();
    }

    public Map<String, Object> toMap() {
        return Map.of(
                "id", id,
                "agentName", agentName,
                "status", status.name().toLowerCase(),
                "input", input != null ? input : "",
                "output", output != null ? output : "",
                "createdAt", createdAt.toString(),
                "updatedAt", updatedAt.toString()
        );
    }
}
