# Module: Agent

## Overview

The Agent class is the primary user-facing API. It wires together model, tools, hooks,
conversation management, and the event loop into a single callable unit.

## Key Classes

### Agent
```java
public class Agent {
    // Construction via builder
    static AgentBuilder builder() { ... }

    // Synchronous invocation
    AgentResult invoke(String prompt);

    // Streaming invocation with callback
    AgentResult invoke(String prompt, StreamHandler handler);

    // Access internals
    List<Message> getMessages();
    AgentState getState();
    ToolRegistry getToolRegistry();
    HookRegistry getHookRegistry();
}
```

### AgentBuilder
Fluent construction:
```java
Agent agent = Agent.builder()
    .model(new BedrockModel("us.anthropic.claude-sonnet-4-6-v1"))
    .systemPrompt("You are a helpful assistant")
    .tools(calculatorTool, weatherTool)
    .conversationManager(new SlidingWindowConversationManager(40))
    .build();
```

### AgentResult
```java
public class AgentResult {
    StopReason stopReason;
    Message message;
    Metrics metrics;
    AgentState state;

    // Convenience: extract text from message
    String toString();
}
```

### AgentState
Key-value store for agent state, JSON-serializable:
```java
public class AgentState {
    void set(String key, Object value);
    <T> T get(String key, Class<T> type);
    Map<String, Object> toMap();
}
```

## ConversationManager

Controls message history size:

### Interface
```java
public interface ConversationManager extends HookProvider {
    void applyManagement(Agent agent);
    void reduceContext(Agent agent, Exception overflow);
}
```

### Implementations
- `SlidingWindowConversationManager(int windowSize)` — trims oldest messages, preserving tool-use/tool-result pairs
- `NullConversationManager` — no trimming (for stateful models or short conversations)

## Agent Lifecycle

1. `Agent.builder().build()`:
   - Initialize ToolRegistry, register tools
   - Initialize HookRegistry
   - Register ConversationManager, SessionManager as HookProviders
   - Fire `AgentInitializedEvent`
2. `agent.invoke("prompt")`:
   - Append user message to messages
   - Run EventLoop
   - Call `conversationManager.applyManagement(agent)`
   - Fire `AfterInvocationEvent`
   - Return `AgentResult`
