# Strands Agents Java SDK - Architecture

## Overview

Java reimplementation of [strands-agents/sdk-python](https://github.com/strands-agents/sdk-python).
A model-driven agent framework that supports tool use, streaming, multi-agent patterns, and extensible model providers.

## Package Structure

```
com.strands
├── types/              # Core data types (Message, ContentBlock, ToolUse, etc.)
│   └── streaming/      # StreamEvent types
├── tool/               # Tool system (@Tool, ToolRegistry, executors)
├── model/              # Model interface and providers
├── hook/               # Typed event hook system
├── event/              # Agentic event loop
├── agent/              # Agent class, ConversationManager
├── session/            # State persistence
├── multiagent/         # Swarm, Graph, Agent-as-Tool
└── plugin/             # Plugin system
```

## Core Flow

```
User calls agent.invoke("prompt")
  → Agent prepends system prompt, appends user message
  → EventLoop.cycle():
      1. Call model.stream(messages, toolSpecs, systemPrompt)
      2. StreamProcessor assembles StreamEvents into Message
      3. If stopReason == TOOL_USE:
           ToolExecutor executes tools concurrently
           Append tool results as user message
           → Recurse to step 1
      4. If stopReason == END_TURN:
           Return AgentResult
  → ConversationManager.applyManagement() (trim/summarize)
  → Return AgentResult to caller
```

## Design Decisions

### Java 17+
- Records for immutable value types where appropriate
- Sealed interfaces where the type hierarchy is closed
- Pattern matching in switch expressions

### Streaming
- `Iterator<StreamEvent>` for synchronous model streaming
- `Flow.Publisher<StreamEvent>` for async/reactive (optional)
- Callback-based alternative via `StreamHandler` interface

### Tool Definition
- `@Tool` annotation on methods (like Python's `@tool` decorator)
- Reflection-based schema generation from method signature
- Manual `AgentTool` interface for advanced use cases

### Async vs Sync
- Primary API is synchronous (blocking) for simplicity
- Async variants via `CompletableFuture` where needed
- Tool execution uses `ExecutorService` for concurrency

### Serialization
- Jackson for JSON (message format, tool schemas, state persistence)
- Compatible with Bedrock Converse API message format
