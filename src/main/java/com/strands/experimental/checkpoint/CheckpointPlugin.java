package com.strands.experimental.checkpoint;

import com.strands.agent.Agent;
import com.strands.hook.HookRegistry;
import com.strands.hook.events.AfterInvocationEvent;
import com.strands.hook.events.BeforeInvocationEvent;
import com.strands.plugin.Hook;
import com.strands.plugin.Plugin;
import com.strands.session.Snapshot;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Plugin that automatically creates checkpoints at configurable points during
 * agent execution. Enables crash-resilient durable execution.
 */
public class CheckpointPlugin extends Plugin {

    private final CheckpointStore store;
    private final String sessionId;
    private Agent agent;
    private int cycleCounter = 0;
    private final int checkpointInterval;

    public CheckpointPlugin(CheckpointStore store, String sessionId) {
        this(store, sessionId, 1);
    }

    public CheckpointPlugin(CheckpointStore store, String sessionId, int checkpointInterval) {
        this.store = store;
        this.sessionId = sessionId;
        this.checkpointInterval = checkpointInterval;
    }

    @Override
    public void initAgent(Agent agent) {
        this.agent = agent;
    }

    @Hook
    public void onBeforeInvocation(BeforeInvocationEvent event) {
        cycleCounter++;
        if (cycleCounter % checkpointInterval == 0) {
            createCheckpoint();
        }
    }

    @Hook
    public void onAfterInvocation(AfterInvocationEvent event) {
        createCheckpoint();
    }

    public void createCheckpoint() {
        if (agent == null) return;

        Snapshot snapshot = agent.takeSnapshot();
        Checkpoint checkpoint = new Checkpoint();
        checkpoint.setCheckpointId(UUID.randomUUID().toString());
        checkpoint.setAgentId(agent.getAgentId());
        checkpoint.setSessionId(sessionId);
        checkpoint.setTimestamp(Instant.now());
        checkpoint.setEventLoopCycle(cycleCounter);
        checkpoint.setAgentState(agent.getState().toMap());
        checkpoint.setSnapshot(Map.of(
                "messages", snapshot.getMessages() != null ? snapshot.getMessages() : java.util.List.of(),
                "systemPrompt", snapshot.getSystemPrompt() != null ? snapshot.getSystemPrompt() : ""
        ));
        checkpoint.setStatus(Checkpoint.CheckpointStatus.ACTIVE);

        store.save(checkpoint);
    }

    public void restoreFromLatest() {
        store.loadLatest(agent.getAgentId(), sessionId).ifPresent(checkpoint -> {
            if (checkpoint.getAgentState() != null) {
                agent.getState().loadFrom(checkpoint.getAgentState());
            }
            cycleCounter = checkpoint.getEventLoopCycle();
        });
    }

    @Override
    public void registerHooks(HookRegistry registry) {
    }
}
