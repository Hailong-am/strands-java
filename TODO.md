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

## Additional Modules (Full Python SDK Parity)

| # | Module | Status | Description |
|---|--------|--------|-------------|
| 11 | `model-openai` | DONE | OpenAIModel with SSE streaming |
| 12 | `model-anthropic` | DONE | AnthropicModel with native streaming |
| 13 | `model-ollama` | DONE | OllamaModel with NDJSON streaming |
| 14 | `tool-mcp` | DONE | MCPClient, MCPTransport, MCPAgentTool |
| 15 | `tool-structured` | DONE | StructuredOutputTool with validation |
| 16 | `interrupt` | DONE | Interrupt/InterruptState for pause/resume |
| 17 | `exceptions` | DONE | Full exception hierarchy |
| 18 | `callback-handler` | DONE | Printing, Composite, Null handlers |
| 19 | `retry` | DONE | ModelRetryStrategy with exponential backoff |
| 20 | `telemetry` | DONE | Tracer, EventLoopMetrics |
| 21 | `guardrails` | DONE | GuardrailConfig, GuardrailAssessment types |
| 22 | `citations` | DONE | Citation, CitationLocation (sealed) |
| 23 | `a2a` | DONE | Agent-to-agent protocol (server, executor) |
| 24 | `session-s3` | DONE | S3SessionManager skeleton |
| 25 | `session-repo` | DONE | RepositorySessionManager with SPI |
| 26 | `summarizing-cm` | DONE | SummarizingConversationManager |
| 27 | `typed-events` | DONE | TypedEvent hierarchy for streaming |
| 28 | `tool-utils` | DONE | ToolValidator, ToolLoader |

## Future Enhancements

| # | Task | Description |
|---|------|-------------|
| 1 | More tests | Unit tests for each module, integration tests |
| 2 | Examples | Usage examples (simple agent, tool use, multi-agent) |
| 3 | Gemini model | GeminiModel provider |
| 4 | LiteLLM model | LiteLLMModel provider |
| 5 | S3 full impl | Wire S3SessionManager to actual AWS S3 client |
| 6 | MCP stdio | StdioMCPTransport implementation |
| 7 | MCP SSE | SSEMCPTransport implementation |
| 8 | Lombok refactor | Apply Lombok to all existing POJOs |

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
