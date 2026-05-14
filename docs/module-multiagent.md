# Module: Multi-Agent

## Overview

Multi-agent patterns allow composing multiple agents for complex tasks.
Three patterns are supported: Agent-as-Tool, Swarm, and Graph.

## Agent-as-Tool

Wraps an Agent as a tool callable by another agent:
```java
AgentTool subAgentTool = AgentAsTool.wrap(subAgent, "researcher", "Researches topics");
Agent orchestrator = Agent.builder()
    .tools(subAgentTool)
    .build();
```

### Behavior
- Parent agent invokes sub-agent via tool use
- Sub-agent runs its own event loop with the prompt as input
- Sub-agent's text output becomes the tool result
- Optional `preserveContext=false` resets sub-agent between calls

## Swarm

Self-organizing agent team with shared conversation context:
```java
public class Swarm implements MultiAgent {
    Swarm(Map<String, Agent> agents, String startAgent);
    AgentResult invoke(String prompt);
}
```

### Behavior
- Agents can hand off to each other via generated handoff tools
- Conversation context flows between agents
- `BeforeNodeCallEvent` / `AfterNodeCallEvent` hooks for monitoring

## Graph

Directed graph execution with conditional edges:
```java
public class GraphAgent implements MultiAgent {
    void addNode(String name, Agent agent);
    void addEdge(String from, String to, Predicate<GraphState> condition);
    AgentResult invoke(String prompt);
}
```

### Behavior
- Nodes execute when all predecessors complete
- Edges can have conditions based on `GraphState`
- Supports parallel execution of independent nodes
- `GraphState` tracks completed/failed/in-progress nodes

## MultiAgent Interface
```java
public interface MultiAgent {
    AgentResult invoke(String prompt);
    // Future: streaming variant
}
```

## MultiAgentResult
Extends `AgentResult` with per-node results:
```java
public class MultiAgentResult extends AgentResult {
    Map<String, NodeResult> nodeResults;
}
```
