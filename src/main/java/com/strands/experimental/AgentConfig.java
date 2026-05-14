package com.strands.experimental;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strands.agent.Agent;
import com.strands.agent.ConcurrentInvocationMode;
import com.strands.model.Model;
import com.strands.model.bedrock.BedrockModel;
import com.strands.model.openai.OpenAIModel;
import com.strands.model.ollama.OllamaModel;
import com.strands.model.litellm.LiteLLMModel;
import com.strands.model.mistral.MistralModel;
import com.strands.model.llamaapi.LlamaAPIModel;
import com.strands.model.llamacpp.LlamaCppModel;
import com.strands.model.writer.WriterModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Creates an Agent from a JSON configuration file. Experimental feature.
 */
public class AgentConfig {

    private static final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public static Agent fromFile(Path configPath) throws IOException {
        String json = Files.readString(configPath);
        return fromJson(json);
    }

    @SuppressWarnings("unchecked")
    public static Agent fromJson(String json) throws IOException {
        Map<String, Object> config = mapper.readValue(json, Map.class);
        return fromMap(config);
    }

    @SuppressWarnings("unchecked")
    public static Agent fromMap(Map<String, Object> config) {
        Agent.Builder builder = Agent.builder();

        if (config.containsKey("agentId")) {
            builder.agentId((String) config.get("agentId"));
        }
        if (config.containsKey("name")) {
            builder.name((String) config.get("name"));
        }
        if (config.containsKey("systemPrompt")) {
            builder.systemPrompt((String) config.get("systemPrompt"));
        }
        if (config.containsKey("concurrentInvocationMode")) {
            String mode = (String) config.get("concurrentInvocationMode");
            builder.concurrentInvocationMode(ConcurrentInvocationMode.valueOf(mode));
        }

        Map<String, Object> modelConfig = (Map<String, Object>) config.get("model");
        if (modelConfig != null) {
            builder.model(createModel(modelConfig));
        } else {
            throw new IllegalArgumentException("Agent config must specify a 'model'");
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static Model createModel(Map<String, Object> config) {
        String provider = (String) config.get("provider");
        String modelId = (String) config.getOrDefault("modelId", "");
        String apiKey = (String) config.getOrDefault("apiKey",
                System.getenv().getOrDefault("MODEL_API_KEY", ""));
        String baseUrl = (String) config.get("baseUrl");

        return switch (provider) {
            case "bedrock" -> new BedrockModel(!modelId.isEmpty() ? modelId : "us.anthropic.claude-sonnet-4-6");
            case "openai" -> baseUrl != null
                    ? new OpenAIModel(apiKey, baseUrl, modelId)
                    : new OpenAIModel(apiKey, modelId);
            case "ollama" -> new OllamaModel(
                    baseUrl != null ? baseUrl : "http://localhost:11434",
                    !modelId.isEmpty() ? modelId : "llama3.1");
            case "litellm" -> baseUrl != null
                    ? new LiteLLMModel(baseUrl, modelId)
                    : new LiteLLMModel("http://localhost:4000", modelId);
            case "mistral" -> new MistralModel(apiKey, modelId);
            case "llamaapi" -> new LlamaAPIModel(apiKey, modelId);
            case "llamacpp" -> new LlamaCppModel(
                    baseUrl != null ? baseUrl : "http://localhost:8080/v1", modelId);
            case "writer" -> new WriterModel(apiKey, modelId);
            default -> throw new IllegalArgumentException("Unknown model provider: " + provider);
        };
    }
}
