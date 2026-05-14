# Module: Plugin System

## Overview

Plugins bundle tools and hooks into reusable packages that initialize with the agent.

## Key Classes

### Plugin
```java
public abstract class Plugin implements HookProvider {
    public void initAgent(Agent agent) { }
    // Methods annotated with @Hook are auto-discovered
    // Methods annotated with @Tool are auto-discovered and registered
}
```

### Plugin Lifecycle

1. `Agent.builder().plugins(myPlugin).build()`
2. During build: `plugin.initAgent(agent)` called
3. Framework scans plugin class for `@Tool` methods → registers them
4. Framework scans plugin class for `@Hook` methods → registers them
5. `plugin.registerHooks(registry)` called for manual hook registration

## Example

```java
public class LoggingPlugin extends Plugin {

    @Hook
    public void onBeforeModelCall(BeforeModelCallEvent event) {
        logger.info("Calling model with {} messages", event.getMessages().size());
    }

    @Hook
    public void onAfterToolCall(AfterToolCallEvent event) {
        logger.info("Tool {} returned: {}", event.getToolName(), event.getResult().getStatus());
    }

    @Tool(name = "log_message", description = "Logs a message")
    public String logMessage(@Param("message") String message) {
        logger.info(message);
        return "logged";
    }
}
```

## Built-in Plugins

- `ModelPlugin` (internal) — handles stateful model cleanup between invocations
