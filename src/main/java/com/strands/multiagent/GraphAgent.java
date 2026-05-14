package com.strands.multiagent;

import com.strands.agent.Agent;
import com.strands.agent.AgentResult;
import com.strands.agent.AgentState;
import com.strands.types.Message;
import com.strands.types.Metrics;
import com.strands.types.StopReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

public class GraphAgent implements MultiAgent {

    private static final Logger log = LoggerFactory.getLogger(GraphAgent.class);

    private final Map<String, Agent> nodes = new LinkedHashMap<>();
    private final List<Edge> edges = new ArrayList<>();
    private final ExecutorService executor;

    public GraphAgent() {
        this(Executors.newCachedThreadPool());
    }

    public GraphAgent(ExecutorService executor) {
        this.executor = executor;
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
        GraphState graphState = new GraphState();
        Map<String, NodeResult> nodeResults = new ConcurrentHashMap<>();

        Set<String> startNodes = findStartNodes();
        if (startNodes.isEmpty() && !nodes.isEmpty()) {
            startNodes = Set.of(nodes.keySet().iterator().next());
        }

        Queue<String> readyQueue = new LinkedList<>(startNodes);
        Set<String> completed = new HashSet<>();

        while (!readyQueue.isEmpty()) {
            List<String> batch = new ArrayList<>(readyQueue);
            readyQueue.clear();

            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (String nodeName : batch) {
                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        Agent agent = nodes.get(nodeName);
                        String nodePrompt = graphState.getNodeInput(nodeName, prompt);
                        log.debug("Graph executing node: {}", nodeName);
                        AgentResult result = agent.invoke(nodePrompt);
                        nodeResults.put(nodeName, NodeResult.completed(nodeName, result));
                        graphState.setNodeOutput(nodeName, result.toString());
                    } catch (Exception e) {
                        log.error("Node {} failed", nodeName, e);
                        nodeResults.put(nodeName, NodeResult.failed(nodeName, e));
                    }
                }, executor));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
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

        return new MultiAgentResult(stopReason, finalMessage, new Metrics(), new AgentState(), nodeResults);
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
