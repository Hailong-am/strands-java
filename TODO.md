# Strands Agents Java SDK - Implementation Plan

## Module Status

| # | Module | Status | Description |
|---|--------|--------|-------------|
| 1 | `types` | DONE | Core data types (Message, ContentBlock, ToolUse, ToolResult, ToolSpec, StreamEvent, etc.) |
| 2 | `tool` | DONE | Tool interface, @Tool annotation, ToolRegistry, ToolExecutor |
| 3 | `model` | DONE | Model interface, streaming contract |
| 4 | `hook` | DONE | Hook system (event registry, hook providers, event types) |
| 5 | `event` | DONE | Event loop (agentic loop, stream processing, retry) |
| 6 | `agent` | DONE | Agent class, AgentResult, ConversationManager |
| 7 | `model-bedrock` | DONE | BedrockModel implementation (AWS SDK) |
| 8 | `session` | DONE | SessionManager interface and file-based impl |
| 9 | `multiagent` | DONE | Multi-agent patterns (Swarm, Graph, Agent-as-Tool) |
| 10 | `plugin` | DONE | Plugin system |

## Remaining Work

| # | Task | Status | Description |
|---|------|--------|-------------|
| 11 | Tests | TODO | Unit tests for each module |
| 12 | Integration test | TODO | End-to-end test with mock model |
| 13 | OpenAI model | TODO | OpenAIModel provider |
| 14 | Anthropic model | TODO | AnthropicModel provider |
| 15 | MCP tools | TODO | MCP protocol tool integration |
| 16 | Retry strategy | TODO | ModelRetryStrategy with backoff |
| 17 | Structured output | TODO | JSON schema-based structured output |
| 18 | Examples | TODO | Usage examples and documentation |

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
