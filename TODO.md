# Strands Agents Java SDK - Implementation Plan

## Module Status

| # | Module | Status | Description |
|---|--------|--------|-------------|
| 1 | `types` | DONE | Core data types (Message, ContentBlock, ToolUse, ToolResult, ToolSpec, StreamEvent, etc.) |
| 2 | `tool` | TODO | Tool interface, @Tool annotation, ToolRegistry, ToolExecutor |
| 3 | `model` | TODO | Model interface, streaming contract |
| 4 | `hook` | TODO | Hook system (event registry, hook providers, event types) |
| 5 | `event` | TODO | Event loop (agentic loop, stream processing, retry) |
| 6 | `agent` | TODO | Agent class, AgentResult, ConversationManager |
| 7 | `model-bedrock` | TODO | BedrockModel implementation (AWS SDK) |
| 8 | `session` | TODO | SessionManager interface and file-based impl |
| 9 | `multiagent` | TODO | Multi-agent patterns (Swarm, Graph, Agent-as-Tool) |
| 10 | `plugin` | TODO | Plugin system |

## Detailed Module Plans

### Module 2: Tool System
- [ ] `AgentTool` interface (name, spec, invoke)
- [ ] `@Tool` annotation for methods
- [ ] `AnnotatedMethodTool` - wraps annotated methods
- [ ] `ToolRegistry` - stores and resolves tools
- [ ] `ToolExecutor` interface
- [ ] `ConcurrentToolExecutor` - parallel tool execution
- [ ] `SequentialToolExecutor` - sequential fallback
- [ ] `ToolContext` - passed to tools during invocation

### Module 3: Model System
- [ ] `Model` interface (stream, getConfig, updateConfig)
- [ ] `ModelResponse` - assembled from stream
- [ ] Stream processing utilities (assemble message from StreamEvents)

### Module 4: Hook System
- [ ] `HookEvent` base class
- [ ] Concrete events: BeforeModelCall, AfterModelCall, BeforeToolCall, AfterToolCall, AgentInitialized, AfterInvocation
- [ ] `HookRegistry` - typed event dispatch
- [ ] `HookProvider` interface
- [ ] `@Hook` annotation

### Module 5: Event Loop
- [ ] `EventLoop` class - orchestrates model call → tool execution → recursion
- [ ] `StreamProcessor` - assembles Message from StreamEvent iterator
- [ ] `ModelRetryStrategy` - retry on transient errors
- [ ] `InvocationState` - mutable context threaded through loop

### Module 6: Agent
- [ ] `Agent` class - main entry point
- [ ] `AgentBuilder` - fluent construction
- [ ] `AgentResult` - return type
- [ ] `ConversationManager` interface
- [ ] `SlidingWindowConversationManager`
- [ ] `NullConversationManager`

### Module 7: Bedrock Model Provider
- [ ] `BedrockModel` using AWS SDK v2
- [ ] Bedrock ConverseStream API integration
- [ ] StreamEvent translation

### Module 8: Session Management
- [ ] `SessionManager` interface
- [ ] `FileSessionManager` - local JSON persistence
- [ ] `Snapshot` - serializable agent state

### Module 9: Multi-Agent
- [ ] `MultiAgentBase` interface
- [ ] `AgentAsTool` adapter
- [ ] `Swarm` - self-organizing agent team
- [ ] `GraphAgent` - directed graph execution

### Module 10: Plugin System
- [ ] `Plugin` abstract class
- [ ] Auto-discovery of @Hook and @Tool methods
- [ ] `Plugin.initAgent(agent)` lifecycle
