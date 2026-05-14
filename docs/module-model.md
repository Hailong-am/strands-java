# Module: Model System

## Overview

The Model interface abstracts LLM providers. All providers must normalize their streaming
output into the SDK's `StreamEvent` format (modeled after AWS Bedrock's ConverseStream API).

## Key Interfaces

### Model
```java
public interface Model {
    Iterator<StreamEvent> stream(StreamRequest request);
    ModelConfig getConfig();
    void updateConfig(Map<String, Object> config);
}
```

### StreamRequest
Groups all inputs to a model call:
```java
public class StreamRequest {
    List<Message> messages;
    List<ToolSpec> toolSpecs;
    String systemPrompt;
    Map<String, Object> modelConfig;  // temperature, maxTokens, etc.
}
```

### ModelConfig
Provider-specific configuration:
```java
public class ModelConfig {
    String modelId;
    Map<String, Object> parameters;  // temperature, topP, maxTokens, etc.
}
```

## StreamEvent Contract

Every model provider must yield `StreamEvent` objects in this sequence:

1. `MESSAGE_START` — `{role: "assistant"}`
2. For each content block:
   - `CONTENT_BLOCK_START` — `{contentBlockIndex, start: {toolUse?: {toolUseId, name}}}`
   - `CONTENT_BLOCK_DELTA` (repeated) — `{contentBlockIndex, delta: {text | toolUse.input}}`
   - `CONTENT_BLOCK_STOP` — `{contentBlockIndex}`
3. `MESSAGE_STOP` — `{stopReason: "end_turn" | "tool_use" | "max_tokens"}`
4. `METADATA` — `{usage: {inputTokens, outputTokens}, metrics: {latencyMs}}`

## Stream Processing

`StreamProcessor` assembles a sequence of `StreamEvent` into:
- A complete `Message` (role + content blocks)
- A `StopReason`
- A `Usage` object

## Planned Providers

| Provider | Class | Dependency |
|----------|-------|------------|
| AWS Bedrock | `BedrockModel` | AWS SDK v2 (bedrock-runtime) |
| OpenAI | `OpenAIModel` | OpenAI Java SDK |
| Anthropic | `AnthropicModel` | Anthropic Java SDK |

Each provider lives in its own Gradle submodule to keep dependencies optional.
