package com.strands.model.anthropic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strands.model.Model;
import com.strands.model.ModelConfig;
import com.strands.model.StreamRequest;
import com.strands.types.*;
import com.strands.types.exceptions.ContextWindowOverflowException;
import com.strands.types.exceptions.ModelThrottledException;
import com.strands.types.streaming.StreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AnthropicModel implements Model {

    private static final Logger log = LoggerFactory.getLogger(AnthropicModel.class);
    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com/v1";
    private static final String DEFAULT_MODEL_ID = "claude-sonnet-4-6-20250514";
    private static final String API_VERSION = "2023-06-01";

    private final String apiKey;
    private final String baseUrl;
    private final ModelConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnthropicModel(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, DEFAULT_MODEL_ID);
    }

    public AnthropicModel(String apiKey, String modelId) {
        this(apiKey, DEFAULT_BASE_URL, modelId);
    }

    public AnthropicModel(String apiKey, String baseUrl, String modelId) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.config = new ModelConfig(modelId);
        if (!config.getParameters().containsKey("maxTokens")) {
            config.setParameter("maxTokens", 4096);
        }
    }

    @Override
    public Iterator<StreamEvent> stream(StreamRequest request) {
        Map<String, Object> body = formatRequest(request);
        List<StreamEvent> events = new ArrayList<>();

        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(baseUrl + "/messages").toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-api-key", apiKey);
            conn.setRequestProperty("anthropic-version", API_VERSION);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(objectMapper.writeValueAsBytes(body));
            }

            int status = conn.getResponseCode();
            if (status == 429) {
                throw new ModelThrottledException("Rate limit exceeded");
            }
            if (status >= 400) {
                String error = readStream(conn);
                if (error.contains("prompt is too long") || error.contains("context window")) {
                    throw new ContextWindowOverflowException(error);
                }
                throw new RuntimeException("Anthropic API error (" + status + "): " + error);
            }

            long startTime = System.currentTimeMillis();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                processSSEStream(reader, events);
            }

            long latency = System.currentTimeMillis() - startTime;
            boolean hasMetadata = events.stream().anyMatch(e -> e.getType() == StreamEvent.Type.METADATA);
            if (!hasMetadata) {
                events.add(StreamEvent.metadata(0, 0, latency));
            }

        } catch (ModelThrottledException | ContextWindowOverflowException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Anthropic stream failed", e);
        }

        return events.iterator();
    }

    @Override
    public ModelConfig getConfig() {
        return config;
    }

    @Override
    public void updateConfig(Map<String, Object> configUpdates) {
        if (configUpdates.containsKey("modelId")) {
            config.setModelId((String) configUpdates.get("modelId"));
        }
        for (Map.Entry<String, Object> entry : configUpdates.entrySet()) {
            if (!"modelId".equals(entry.getKey())) {
                config.setParameter(entry.getKey(), entry.getValue());
            }
        }
    }

    private Map<String, Object> formatRequest(StreamRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelId());
        body.put("stream", true);
        body.put("max_tokens", config.getParameters().getOrDefault("maxTokens", 4096));

        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            body.put("system", request.getSystemPrompt());
        }

        body.put("messages", formatMessages(request.getMessages()));

        if (request.getToolSpecs() != null && !request.getToolSpecs().isEmpty()) {
            body.put("tools", formatTools(request.getToolSpecs()));
        }

        Map<String, Object> params = config.getParameters();
        if (params.containsKey("temperature")) body.put("temperature", params.get("temperature"));
        if (params.containsKey("topP")) body.put("top_p", params.get("topP"));

        return body;
    }

    private List<Map<String, Object>> formatMessages(List<Message> messages) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Message msg : messages) {
            String role = msg.getRole() == Message.Role.USER ? "user" : "assistant";
            List<Map<String, Object>> content = new ArrayList<>();

            for (ContentBlock block : msg.getContent()) {
                if (block.isText()) {
                    content.add(Map.of("type", "text", "text", block.getText()));
                } else if (block.isToolUse()) {
                    ToolUse tu = block.getToolUse();
                    Map<String, Object> toolUseBlock = new LinkedHashMap<>();
                    toolUseBlock.put("type", "tool_use");
                    toolUseBlock.put("id", tu.getToolUseId());
                    toolUseBlock.put("name", tu.getName());
                    toolUseBlock.put("input", tu.getInput() != null ? tu.getInput() : Map.of());
                    content.add(toolUseBlock);
                } else if (block.isToolResult()) {
                    ToolResult tr = block.getToolResult();
                    Map<String, Object> toolResultBlock = new LinkedHashMap<>();
                    toolResultBlock.put("type", "tool_result");
                    toolResultBlock.put("tool_use_id", tr.getToolUseId());
                    toolResultBlock.put("is_error", tr.getStatus() == ToolResult.Status.ERROR);

                    List<Map<String, Object>> resultContent = new ArrayList<>();
                    if (tr.getContent() != null) {
                        for (ToolResultContent c : tr.getContent()) {
                            if (c.getText() != null) {
                                resultContent.add(Map.of("type", "text", "text", c.getText()));
                            }
                        }
                    }
                    toolResultBlock.put("content", resultContent);
                    content.add(toolResultBlock);
                }
            }

            result.add(Map.of("role", role, "content", content));
        }

        return result;
    }

    private List<Map<String, Object>> formatTools(List<ToolSpec> toolSpecs) {
        List<Map<String, Object>> tools = new ArrayList<>();
        for (ToolSpec spec : toolSpecs) {
            Map<String, Object> tool = new LinkedHashMap<>();
            tool.put("name", spec.getName());
            tool.put("description", spec.getDescription() != null ? spec.getDescription() : "");
            tool.put("input_schema", spec.getInputSchema() != null ? spec.getInputSchema() : Map.of("type", "object"));
            tools.add(tool);
        }
        return tools;
    }

    @SuppressWarnings("unchecked")
    private void processSSEStream(BufferedReader reader, List<StreamEvent> events) throws Exception {
        int contentBlockIndex = 0;

        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.startsWith("data: ")) continue;
            String data = line.substring(6).trim();

            Map<String, Object> event = objectMapper.readValue(data, Map.class);
            String type = (String) event.get("type");
            if (type == null) continue;

            switch (type) {
                case "message_start" -> events.add(StreamEvent.messageStart("assistant"));

                case "content_block_start" -> {
                    contentBlockIndex = ((Number) event.get("index")).intValue();
                    Map<String, Object> contentBlock = (Map<String, Object>) event.get("content_block");
                    String blockType = contentBlock != null ? (String) contentBlock.get("type") : "text";

                    if ("tool_use".equals(blockType)) {
                        events.add(StreamEvent.contentBlockStart(contentBlockIndex, Map.of(
                                "toolUse", Map.of(
                                        "toolUseId", contentBlock.get("id"),
                                        "name", contentBlock.get("name")
                                ))));
                    } else {
                        events.add(StreamEvent.contentBlockStart(contentBlockIndex, Map.of()));
                    }
                }

                case "content_block_delta" -> {
                    int idx = ((Number) event.get("index")).intValue();
                    Map<String, Object> delta = (Map<String, Object>) event.get("delta");
                    String deltaType = (String) delta.get("type");

                    if ("text_delta".equals(deltaType)) {
                        events.add(StreamEvent.contentBlockDelta(idx, Map.of("text", delta.get("text"))));
                    } else if ("input_json_delta".equals(deltaType)) {
                        events.add(StreamEvent.contentBlockDelta(idx, Map.of(
                                "toolUse", Map.of("input", delta.get("partial_json")))));
                    } else if ("thinking_delta".equals(deltaType)) {
                        events.add(StreamEvent.contentBlockDelta(idx, Map.of(
                                "reasoningContent", Map.of("text", delta.get("thinking")))));
                    }
                }

                case "content_block_stop" -> {
                    int idx = ((Number) event.get("index")).intValue();
                    events.add(StreamEvent.contentBlockStop(idx));
                }

                case "message_delta" -> {
                    Map<String, Object> delta = (Map<String, Object>) event.get("delta");
                    String stopReason = (String) delta.get("stop_reason");
                    if (stopReason != null) {
                        String normalized = switch (stopReason) {
                            case "tool_use" -> "tool_use";
                            case "max_tokens" -> "max_tokens";
                            default -> "end_turn";
                        };
                        events.add(StreamEvent.messageStop(normalized));
                    }
                    Map<String, Object> usage = (Map<String, Object>) event.get("usage");
                    if (usage != null) {
                        long output = ((Number) usage.getOrDefault("output_tokens", 0)).longValue();
                        events.add(StreamEvent.metadata(0, output, 0));
                    }
                }

                case "message_stop" -> {
                    // Final event, usage already handled in message_delta
                }
            }
        }
    }

    private String readStream(HttpURLConnection conn) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String l;
            while ((l = reader.readLine()) != null) sb.append(l);
            return sb.toString();
        } catch (Exception e) {
            return "Unknown error";
        }
    }
}
