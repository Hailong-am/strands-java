package com.strands.multiagent;

import java.time.Instant;

/**
 * Streaming event emitted by GraphAgent during execution.
 * Listeners receive these events to track graph execution progress.
 */
public class GraphEvent {

    public enum Type {
        NODE_START, NODE_STOP, HANDOFF, GRAPH_START, GRAPH_STOP
    }

    private final Type type;
    private final String nodeName;
    private final boolean success;
    private final Instant timestamp;
    private final String targetNode;

    private GraphEvent(Type type, String nodeName, boolean success, String targetNode) {
        this.type = type;
        this.nodeName = nodeName;
        this.success = success;
        this.timestamp = Instant.now();
        this.targetNode = targetNode;
    }

    public static GraphEvent nodeStart(String nodeName) {
        return new GraphEvent(Type.NODE_START, nodeName, true, null);
    }

    public static GraphEvent nodeStop(String nodeName, boolean success) {
        return new GraphEvent(Type.NODE_STOP, nodeName, success, null);
    }

    public static GraphEvent handoff(String fromNode, String toNode) {
        return new GraphEvent(Type.HANDOFF, fromNode, true, toNode);
    }

    public static GraphEvent graphStart() {
        return new GraphEvent(Type.GRAPH_START, null, true, null);
    }

    public static GraphEvent graphStop(boolean success) {
        return new GraphEvent(Type.GRAPH_STOP, null, success, null);
    }

    public Type getType() { return type; }
    public String getNodeName() { return nodeName; }
    public boolean isSuccess() { return success; }
    public Instant getTimestamp() { return timestamp; }
    public String getTargetNode() { return targetNode; }
}
