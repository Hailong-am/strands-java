# Module: Hook System

## Overview

Typed event hooks allow plugins, session managers, and conversation managers to observe
and modify agent behavior at key lifecycle points without tight coupling.

## Key Interfaces

### HookEvent
Base class for all hook events. Each event is a concrete class with typed fields.

### HookProvider
Interface for components that register hooks:
```java
public interface HookProvider {
    void registerHooks(HookRegistry registry);
}
```

### HookRegistry
Central event dispatch:
```java
public class HookRegistry {
    <E extends HookEvent> void register(Class<E> eventType, HookHandler<E> handler);
    <E extends HookEvent> void emit(E event);
}
```

### HookHandler
```java
@FunctionalInterface
public interface HookHandler<E extends HookEvent> {
    void handle(E event);
}
```

## Hook Events

| Event | When Fired | Mutable Fields |
|-------|-----------|----------------|
| `AgentInitializedEvent` | After agent construction | — |
| `BeforeModelCallEvent` | Before calling model.stream() | messages (can modify) |
| `AfterModelCallEvent` | After model stream completes | retry flag |
| `BeforeToolCallEvent` | Before tool invocation | selectedTool (can swap), cancel flag |
| `AfterToolCallEvent` | After tool returns | retry flag |
| `AfterInvocationEvent` | After event loop completes | resume flag, resumeInput |
| `MessageAddedEvent` | When message appended to history | — |

## Hook Priority

Handlers execute in registration order. A handler can short-circuit by setting
`event.setConsumed(true)` — subsequent handlers for the same event type are skipped.

## Usage Example

```java
agent.getHookRegistry().register(BeforeToolCallEvent.class, event -> {
    if (event.getToolName().equals("dangerous_tool")) {
        event.setCancelled(true);
    }
});
```
