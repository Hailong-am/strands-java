package com.strands.experimental.checkpoint;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a durable execution checkpoint. Captures the full agent state
 * at a point in time so execution can resume after crashes or restarts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Checkpoint {

    private String checkpointId;
    private String agentId;
    private String sessionId;
    private Instant timestamp;
    private int eventLoopCycle;
    private Map<String, Object> agentState;
    private Map<String, Object> snapshot;
    private CheckpointStatus status;

    public enum CheckpointStatus {
        ACTIVE, COMPLETED, FAILED, EXPIRED
    }
}
