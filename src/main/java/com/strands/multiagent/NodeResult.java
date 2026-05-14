package com.strands.multiagent;

import com.strands.agent.AgentResult;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NodeResult {

    public enum Status {
        COMPLETED, FAILED, INTERRUPTED
    }

    private final String nodeName;
    private final Status status;
    private final AgentResult result;
    private final Exception error;

    public static NodeResult completed(String nodeName, AgentResult result) {
        return new NodeResult(nodeName, Status.COMPLETED, result, null);
    }

    public static NodeResult failed(String nodeName, Exception error) {
        return new NodeResult(nodeName, Status.FAILED, null, error);
    }
}
