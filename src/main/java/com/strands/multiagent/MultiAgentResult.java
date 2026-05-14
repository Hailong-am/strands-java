package com.strands.multiagent;

import com.strands.agent.AgentResult;
import com.strands.agent.AgentState;
import com.strands.types.Message;
import com.strands.types.Metrics;
import com.strands.types.StopReason;

import java.util.Map;

public class MultiAgentResult extends AgentResult {

    private final Map<String, NodeResult> nodeResults;

    public MultiAgentResult(StopReason stopReason, Message message, Metrics metrics,
                            AgentState state, Map<String, NodeResult> nodeResults) {
        super(stopReason, message, metrics, state);
        this.nodeResults = nodeResults;
    }

    public Map<String, NodeResult> getNodeResults() {
        return nodeResults;
    }

    public boolean allCompleted() {
        return nodeResults.values().stream()
                .allMatch(r -> r.getStatus() == NodeResult.Status.COMPLETED);
    }
}
