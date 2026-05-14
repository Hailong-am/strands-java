package com.strands.multiagent;

import com.strands.agent.AgentResult;

public class NodeResult {

    public enum Status {
        COMPLETED, FAILED, INTERRUPTED
    }

    private final String nodeName;
    private final Status status;
    private final AgentResult result;
    private final Exception error;

    public NodeResult(String nodeName, Status status, AgentResult result, Exception error) {
        this.nodeName = nodeName;
        this.status = status;
        this.result = result;
        this.error = error;
    }

    public static NodeResult completed(String nodeName, AgentResult result) {
        return new NodeResult(nodeName, Status.COMPLETED, result, null);
    }

    public static NodeResult failed(String nodeName, Exception error) {
        return new NodeResult(nodeName, Status.FAILED, null, error);
    }

    public String getNodeName() {
        return nodeName;
    }

    public Status getStatus() {
        return status;
    }

    public AgentResult getResult() {
        return result;
    }

    public Exception getError() {
        return error;
    }
}
