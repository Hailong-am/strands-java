# Strands Agents SDK for Java

A Java implementation of the [Strands Agents SDK](https://github.com/strands-agents/sdk-python), providing a model-driven approach to building AI agents with tool use, multi-agent orchestration, and streaming capabilities.

## Requirements

- Java 17+
- Gradle 8.7+

## Quick Start

```java
import com.strands.agent.Agent;
import com.strands.agent.AgentResult;
import com.strands.model.bedrock.BedrockModel;

Agent agent = Agent.builder()
    .model(new BedrockModel("us.anthropic.claude-sonnet-4-6"))
    .systemPrompt("You are a helpful assistant.")
    .build();

AgentResult result = agent.invoke("What is the capital of France?");
System.out.println(result);
```

## Features

### Model Providers

| Provider | Class | Description |
|----------|-------|-------------|
| Amazon Bedrock | `BedrockModel` | Converse API with native streaming |
| Amazon Bedrock (OpenAI-compat) | `BedrockMantleModel` | Bedrock via OpenAI message format |
| OpenAI | `OpenAIModel` | GPT-4o, o1, and compatible APIs |
| Anthropic | `AnthropicModel` | Direct Claude API |
| Ollama | `OllamaModel` | Local models via Ollama |
| LiteLLM | `LiteLLMModel` | LiteLLM proxy gateway |
| Mistral | `MistralModel` | Mistral AI models |
| Llama API | `LlamaAPIModel` | Meta Llama hosted API |
| Llama.cpp | `LlamaCppModel` | Local llama.cpp server |
| SageMaker | `SageMakerModel` | SageMaker endpoints |
| Gemini | `GeminiModel` | Google Gemini models |
| Writer | `WriterModel` | Writer AI platform |

### Tool System

Define tools with annotations or implement `AgentTool`:

```java
public class MyTools {
    @Tool(description = "Get the current weather for a city")
    public String getWeather(@ToolParam("city") String city) {
        return "Sunny, 72F in " + city;
    }
}

Agent agent = Agent.builder()
    .model(new BedrockModel())
    .toolProviders(new MyTools())
    .build();
```

Additional tool features:
- **MCP Integration** - Connect to MCP servers via stdio, SSE, or HTTP streaming transports
- **ToolChoice** - Control tool selection (auto/any/specific tool/none)
- **ToolGenerator** - Tools that yield intermediate streaming events
- **Structured Output** - Type-safe structured responses via `agent.structuredOutput(MyClass.class, prompt)`

### Multi-Agent Orchestration

**Swarm** - Agent handoff with shared working memory:

```java
Swarm swarm = new Swarm(Map.of(
    "triage", triageAgent,
    "billing", billingAgent,
    "support", supportAgent
), "triage");

AgentResult result = swarm.invoke("I need help with my bill");
```

**GraphAgent** - DAG-based parallel execution with conditions:

```java
GraphAgent graph = new GraphAgent();
graph.addNode("research", researchAgent);
graph.addNode("write", writerAgent);
graph.addNode("review", reviewAgent);
graph.addEdge("research", "write");
graph.addEdge("write", "review");
graph.setNodeTimeout(30000); // 30s per node

AgentResult result = graph.invoke("Write a report on AI trends");
```

**A2A Protocol** - Agent-to-Agent communication:

```java
A2AServer server = new A2AServer("localhost", 8080);
server.register("assistant", myAgent);
A2ARequestHandler handler = new A2ARequestHandler(server);
```

### Plugin System

Extend agent behavior with plugins:

```java
Agent agent = Agent.builder()
    .model(model)
    .plugins(
        new SteeringPlugin(mySteeringHandler),
        new AgentSkillsPlugin(skillSource),
        new ContextOffloaderPlugin(new FileStorage(path))
    )
    .build();
```

**Steering Plugin** - Runtime guidance with LLM-based evaluation:

```java
SteeringPlugin steering = new SteeringPlugin(new LLMSteeringHandler(guardModel));
steering.addProvider(myContextProvider);
// Returns SteeringAction: Proceed, Guide (inject guidance), or Interrupt
```

### Hook System

Intercept and modify agent behavior at key points:

```java
hookRegistry.register(BeforeToolCallEvent.class, event -> {
    if (event.getToolName().equals("dangerous_tool")) {
        event.setCancelled(true);
    }
});

hookRegistry.register(AfterModelCallEvent.class, event -> {
    if (needsRetry(event.getMessage())) {
        event.setRetry(true);
    }
});
```

Available hook events:
- `BeforeInvocationEvent` / `AfterInvocationEvent`
- `BeforeModelCallEvent` / `AfterModelCallEvent`
- `BeforeToolCallEvent` / `AfterToolCallEvent`
- `BeforeNodeCallEvent` / `AfterNodeCallEvent`
- `MessageAddedEvent`, `AgentInitializedEvent`

### Bidirectional Streaming (Experimental)

Real-time voice and text with Nova Sonic, Gemini Live, or OpenAI Realtime:

```java
BidiAgent bidiAgent = BidiAgent.builder()
    .model(new BidiNovaSonicModel())
    .tools(myTools)
    .systemPrompt("You are a voice assistant.")
    .build();

BidiStreamSession session = bidiAgent.start(myHandler);
session.sendText("Hello!");
session.sendAudio(audioBytes, "pcm16");
```

### Checkpointing (Experimental)

Durable execution with crash recovery:

```java
CheckpointStore store = new FileCheckpointStore(Path.of("./checkpoints"));
CheckpointPlugin plugin = new CheckpointPlugin(store);

Agent agent = Agent.builder()
    .model(model)
    .plugins(plugin)
    .build();
```

### Telemetry

OpenTelemetry integration with OTLP export:

```java
StrandsTelemetry.instance()
    .otlpEndpoint("http://localhost:4317")
    .serviceName("my-agent")
    .enable();
```

Supports:
- Distributed tracing with W3C Trace Context propagation
- Metrics (token usage, tool call counts, latency histograms)
- Automatic span creation for model calls, tool executions, and event loop cycles

## Project Structure

```
src/main/java/com/strands/
  agent/          - Core Agent, AgentResult, ConversationManager
  event/          - EventLoop, stream processing
  handler/        - Callback handlers for streaming
  hook/           - Hook system (registry, events, providers)
  model/          - Model interface and provider implementations
  multiagent/     - Swarm, GraphAgent, A2A protocol
  plugin/         - Plugin system (steering, skills, offloader)
  session/        - Session management, snapshots
  telemetry/      - OpenTelemetry tracing and metrics
  tool/           - Tool system, MCP client, structured output
  types/          - Core types (Message, ContentBlock, ToolUse, etc.)
  experimental/   - Bidi streaming, checkpointing
```

## Building

```bash
./gradlew build
```

## Running Examples

```bash
./gradlew runExample -Pexample=examples.SimpleAgent
```

## Running Tests

```bash
./gradlew test
```

## License

Apache License 2.0
