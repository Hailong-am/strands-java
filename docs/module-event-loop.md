# Module: Event Loop

## Overview

The event loop is the core agentic cycle: call model → process response → execute tools → repeat.
It is iterative (not recursive) to avoid deep call stacks in Java.

## Key Classes

### EventLoop
```java
public class EventLoop {
    AgentResult run(Agent agent, InvocationState state);
}
```

### InvocationState
Mutable context threaded through the entire loop cycle:
```java
public class InvocationState {
    Map<String, Object> properties;   // user-defined pass-through state
    Metrics metrics;                  // accumulated usage/latency
}
```

### StreamProcessor
Assembles raw `StreamEvent` iterator into a complete model response:
```java
public class StreamProcessor {
    StreamResult process(Iterator<StreamEvent> stream, StreamHandler handler);
}
```

### StreamResult
```java
public class StreamResult {
    Message message;       // assembled assistant message
    StopReason stopReason;
    Usage usage;
}
```

### StreamHandler (Callback)
```java
public interface StreamHandler {
    void onTextDelta(String delta);
    void onToolUseStart(String toolName, String toolUseId);
    void onToolUseDelta(String delta);
    void onComplete(Message message, StopReason stopReason);
}
```

## Event Loop Algorithm

```
EventLoop.run(agent, state):
  while true:
    1. Fire BeforeModelCallEvent
    2. Call model.stream(messages, toolSpecs, systemPrompt)
    3. StreamProcessor.process(stream) → StreamResult
    4. Fire AfterModelCallEvent
       - If retry requested: goto 1
    5. Append assistant message to agent.messages
    6. If stopReason == TOOL_USE:
         Extract toolUses from message
         ToolExecutor.execute(toolUses) → List<ToolResult>
         Build toolResult message, append to agent.messages
         Continue loop (goto 1)
    7. If stopReason == END_TURN:
         Return AgentResult(message, stopReason, metrics)
    8. If stopReason == MAX_TOKENS:
         Return AgentResult (caller decides to continue or not)
```

## Retry Strategy

`ModelRetryStrategy` handles transient model errors:
- Configurable max retries and backoff
- Fires via `AfterModelCallEvent` hook
- Retries on throttling (429) and server errors (5xx)

## Concurrency Model

- Model streaming is synchronous (blocking I/O on the calling thread)
- Tool execution uses `ExecutorService` for parallelism
- The event loop itself runs on a single thread per invocation
