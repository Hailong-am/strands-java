package com.strands.multiagent;

import com.strands.agent.Agent;
import com.strands.agent.AgentResult;
import com.strands.agent.AgentState;
import com.strands.hook.HookRegistry;
import com.strands.hook.events.BeforeNodeCallEvent;
import com.strands.hook.events.AfterNodeCallEvent;
import com.strands.hook.events.BeforeMultiAgentInvocationEvent;
import com.strands.hook.events.AfterMultiAgentInvocationEvent;
import com.strands.session.SessionManager;
import com.strands.types.Message;
import com.strands.types.Metrics;
import com.strands.types.StopReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class GraphAgent implements MultiAgent {

    private static final Logger log = LoggerFactory.getLogger(GraphAgent.class);

    private final Map<String, Agent> nodes = new LinkedHashMap<>();
    private final List<Edge> edges = new ArrayList<>();
    private final ExecutorService executor;
    private final HookRegistry hookRegistry;
    private final List<Consumer<GraphEvent>> eventListeners = new ArrayList<>();
    private SessionManager sessionManager;
    private long nodeTimeoutMs = 0;
    private volatile boolean interrupted;
    private String interruptedAtNode;
    private GraphState interruptedState;

    public GraphAgent() {
        this(Executors.newCachedThreadPool(), new HookRegistry());
    }

    public GraphAgent(ExecutorService executor) {
        this(executor, new HookRegistry());
    }

    public GraphAgent(ExecutorService executor, HookRegistry hookRegistry) {
        this.executor = executor;
        this.hookRegistry = hookRegistry;
    }

    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public HookRegistry getHookRegistry() {
        return hookRegistry;
    }

    public void setNodeTimeout(long timeoutMs) {
        this.nodeTimeoutMs = timeoutMs;
    }

    public void addEventListener(Consumer<GraphEvent> listener) {
        eventListeners.add(listener);
    }

    public void interrupt() {
        this.interrupted = true;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public void addNode(String name, Agent agent) {
        nodes.put(name, agent);
    }

    public void addEdge(String from, String to) {
        edges.add(new Edge(from, to, null));
    }

    public void addEdge(String from, String to, Predicate<GraphState> condition) {
        edges.add(new Edge(from, to, condition));
    }

    @Override
    public AgentResult invoke(String prompt) {
        hookRegistry.emit(new BeforeMultiAgentInvocationEvent(this, Map.of("prompt", prompt)));

        GraphState graphState = new GraphState();
        Map<String, NodeResult> nodeResults = new ConcurrentHashMap<>();
        interrupted = false;

        Set<String> startNodes = findStartNodes();
        if (startNodes.isEmpty() && !nodes.isEmpty()) {
            startNodes = Set.of(nodes.keySet().iterator().next());
        }

        Queue<String> readyQueue = new LinkedList<>(startNodes);
        Set<String> completed = new HashSet<>();

        while (!readyQueue.isEmpty()) {
            if (interrupted) {
                interruptedAtNode = readyQueue.peek();
                interruptedState = graphState;
                log.info("Graph interrupted at node: {}", interruptedAtNode);
                break;
            }

            List<String> batch = new ArrayList<>(readyQueue);
            readyQueue.clear();

            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (String nodeName : batch) {
                futures.add(CompletableFuture.runAsync(() -> {
                    emitEvent(GraphEvent.nodeStart(nodeName));
                    BeforeNodeCallEvent beforeEvent = new BeforeNodeCallEvent(nodeName, nodes.get(nodeName));
                    hookRegistry.emit(beforeEvent);

                    if (beforeEvent.isCancelled()) {
                        nodeResults.put(nodeName, NodeResult.failed(nodeName,
                                new RuntimeException("Cancelled by hook")));
                        emitEvent(GraphEvent.nodeStop(nodeName, false));
                        return;
                    }

                    try {
                        Agent agent = nodes.get(nodeName);
                        String nodePrompt = graphState.getNodeInput(nodeName, prompt);
                        log.debug("Graph executing node: {}", nodeName);
                        AgentResult result = agent.invoke(nodePrompt);
                        NodeResult nr = NodeResult.completed(nodeName, result);
                        nodeResults.put(nodeName, nr);
                        graphState.setNodeOutput(nodeName, result.toString());

                        hookRegistry.emit(new AfterNodeCallEvent(nodeName, nr));
                        emitEvent(GraphEvent.nodeStop(nodeName, true));
                    } catch (Exception e) {
                        log.error("Node {} failed", nodeName, e);
                        NodeResult nr = NodeResult.failed(nodeName, e);
                        nodeResults.put(nodeName, nr);
                        hookRegistry.emit(new AfterNodeCallEvent(nodeName, nr));
                        emitEvent(GraphEvent.nodeStop(nodeName, false));
                    }
                }, executor));
            }

            try {
                if (nodeTimeoutMs > 0) {
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .get(nodeTimeoutMs, TimeUnit.MILLISECONDS);
                } else {
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                }
            } catch (TimeoutException e) {
                log.error("Node batch timed out after {}ms", nodeTimeoutMs);
                for (String nodeName : batch) {
                    if (!nodeResults.containsKey(nodeName)) {
                        nodeResults.put(nodeName, NodeResult.failed(nodeName,
                                new TimeoutException("Node timed out after " + nodeTimeoutMs + "ms")));
                        emitEvent(GraphEvent.nodeStop(nodeName, false));
                    }
                }
            } catch (Exception e) {
                log.error("Node batch execution failed", e);
            }
            completed.addAll(batch);

            for (Edge edge : edges) {
                if (completed.contains(edge.from) && !completed.contains(edge.to)) {
                    if (edge.condition == null || edge.condition.test(graphState)) {
                        if (allPredecessorsComplete(edge.to, completed)) {
                            readyQueue.add(edge.to);
                        }
                    }
                }
            }
        }

        NodeResult lastResult = null;
        for (String nodeName : nodes.keySet()) {
            if (nodeResults.containsKey(nodeName)) {
                lastResult = nodeResults.get(nodeName);
            }
        }

        Message finalMessage = lastResult != null && lastResult.getResult() != null
                ? lastResult.getResult().getMessage() : null;
        StopReason stopReason = lastResult != null && lastResult.getResult() != null
                ? lastResult.getResult().getStopReason() : StopReason.END_TURN;

        MultiAgentResult multiResult = new MultiAgentResult(stopReason, finalMessage, new Metrics(), new AgentState(), nodeResults);
        hookRegistry.emit(new AfterMultiAgentInvocationEvent(this, multiResult));

        return multiResult;
    }

    /**
     * Resume execution from the point of interruption with a new prompt.
     */
    public AgentResult resume(String prompt) {
        if (interruptedAtNode == null) {
            throw new IllegalStateException("Graph was not interrupted — nothing to resume");
        }
        interrupted = false;
        String resumeNode = interruptedAtNode;
        interruptedAtNode = null;

        if (interruptedState != null) {
            interruptedState.setNodeInput(resumeNode, prompt);
        }
        return invoke(prompt);
    }

    private Set<String> findStartNodes() {
        Set<String> targets = new HashSet<>();
        for (Edge edge : edges) {
            targets.add(edge.to);
        }
        Set<String> startNodes = new LinkedHashSet<>();
        for (String node : nodes.keySet()) {
            if (!targets.contains(node)) {
                startNodes.add(node);
            }
        }
        return startNodes;
    }

    private boolean allPredecessorsComplete(String node, Set<String> completed) {
        for (Edge edge : edges) {
            if (edge.to.equals(node) && !completed.contains(edge.from)) {
                return false;
            }
        }
        return true;
    }

    private void emitEvent(GraphEvent event) {
        for (Consumer<GraphEvent> listener : eventListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                log.warn("Event listener error", e);
            }
        }
    }

    private record Edge(String from, String to, Predicate<GraphState> condition) {
    }

    public static class GraphState {
        private final Map<String, String> nodeOutputs = new ConcurrentHashMap<>();
        private final Map<String, String> nodeInputOverrides = new ConcurrentHashMap<>();

        public void setNodeOutput(String nodeName, String output) {
            nodeOutputs.put(nodeName, output);
        }

        public String getNodeOutput(String nodeName) {
            return nodeOutputs.get(nodeName);
        }

        public Map<String, String> getAllOutputs() {
            return Collections.unmodifiableMap(nodeOutputs);
        }

        public void setNodeInput(String nodeName, String input) {
            nodeInputOverrides.put(nodeName, input);
        }

        public String getNodeInput(String nodeName, String defaultInput) {
            return nodeInputOverrides.getOrDefault(nodeName, defaultInput);
        }
    }
}
