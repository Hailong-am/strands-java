package com.strands.model.openai;

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

public class OpenAIModel implements Model {

    private static final Logger log = LoggerFactory.getLogger(OpenAIModel.class);
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final String DEFAULT_MODEL_ID = "gpt-4o";

    private final String apiKey;
    private final String baseUrl;
    private final ModelConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAIModel(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, DEFAULT_MODEL_ID);
    }

    public OpenAIModel(String apiKey, String modelId) {
        this(apiKey, DEFAULT_BASE_URL, modelId);
    }

    public OpenAIModel(String apiKey, String baseUrl, String modelId) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.config = new ModelConfig(modelId);
    }

    @Override
    public Iterator<StreamEvent> stream(StreamRequest request) {
        Map<String, Object> body = formatRequest(request);
        List<StreamEvent> events = new ArrayList<>();

        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(baseUrl + "/chat/completions").toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
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
                if (error.contains("context_length_exceeded") || error.contains("maximum context length")) {
                    throw new ContextWindowOverflowException(error);
                }
                throw new RuntimeException("OpenAI API error (" + status + "): " + error);
            }

            events.add(StreamEvent.messageStart("assistant"));
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
            throw new RuntimeException("OpenAI stream failed", e);
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
        body.put("stream_options", Map.of("include_usage", true));

        List<Map<String, Object>> messages = formatMessages(request);
        body.put("messages", messages);

        if (request.getToolSpecs() != null && !request.getToolSpecs().isEmpty()) {
            body.put("tools", formatTools(request.getToolSpecs()));
        }

        Map<String, Object> params = config.getParameters();
        if (params.containsKey("temperature")) body.put("temperature", params.get("temperature"));
        if (params.containsKey("maxTokens")) body.put("max_tokens", params.get("maxTokens"));
        if (params.containsKey("topP")) body.put("top_p", params.get("topP"));

        return body;
    }

    private List<Map<String, Object>> formatMessages(StreamRequest request) {
        List<Map<String, Object>> messages = new ArrayList<>();

        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            messages.add(Map.of("role", "system", "content", request.getSystemPrompt()));
        }

        for (Message msg : request.getMessages()) {
            if (msg.hasToolUse() && msg.getRole() == Message.Role.ASSISTANT) {
                Map<String, Object> assistantMsg = new LinkedHashMap<>();
                assistantMsg.put("role", "assistant");

                List<Map<String, Object>> content = new ArrayList<>();
                List<Map<String, Object>> toolCalls = new ArrayList<>();

                for (ContentBlock block : msg.getContent()) {
                    if (block.isText()) {
                        content.add(Map.of("type", "text", "text", block.getText()));
                    } else if (block.isToolUse()) {
                        ToolUse tu = block.getToolUse();
                        Map<String, Object> tc = new LinkedHashMap<>();
                        tc.put("id", tu.getToolUseId());
                        tc.put("type", "function");
                        tc.put("function", Map.of(
                                "name", tu.getName(),
                                "arguments", toJson(tu.getInput())
                        ));
                        toolCalls.add(tc);
                    }
                }

                if (!content.isEmpty()) {
                    assistantMsg.put("content", content);
                }
                if (!toolCalls.isEmpty()) {
                    assistantMsg.put("tool_calls", toolCalls);
                }
                messages.add(assistantMsg);

            } else if (msg.getContent().stream().anyMatch(ContentBlock::isToolResult)) {
                for (ContentBlock block : msg.getContent()) {
                    if (block.isToolResult()) {
                        ToolResult tr = block.getToolResult();
                        StringBuilder text = new StringBuilder();
                        if (tr.getContent() != null) {
                            for (ToolResultContent c : tr.getContent()) {
                                if (c.getText() != null) {
                                    if (text.length() > 0) text.append("\n");
                                    text.append(c.getText());
                                }
                            }
                        }
                        messages.add(Map.of(
                                "role", "tool",
                                "tool_call_id", tr.getToolUseId(),
                                "content", text.toString()
                        ));
                    }
                }
            } else {
                String role = msg.getRole() == Message.Role.USER ? "user" : "assistant";
                List<Map<String, Object>> content = new ArrayList<>();
                for (ContentBlock block : msg.getContent()) {
                    if (block.isText()) {
                        content.add(Map.of("type", "text", "text", block.getText()));
                    }
                }
                if (content.size() == 1) {
                    messages.add(Map.of("role", role, "content", content.get(0).get("text")));
                } else {
                    messages.add(Map.of("role", role, "content", content));
                }
            }
        }

        return messages;
    }

    private List<Map<String, Object>> formatTools(List<ToolSpec> toolSpecs) {
        List<Map<String, Object>> tools = new ArrayList<>();
        for (ToolSpec spec : toolSpecs) {
            tools.add(Map.of(
                    "type", "function",
                    "function", Map.of(
                            "name", spec.getName(),
                            "description", spec.getDescription() != null ? spec.getDescription() : "",
                            "parameters", spec.getInputSchema() != null ? spec.getInputSchema() : Map.of()
                    )
            ));
        }
        return tools;
    }

    @SuppressWarnings("unchecked")
    private void processSSEStream(BufferedReader reader, List<StreamEvent> events) throws Exception {
        int contentBlockIndex = 0;
        boolean textStarted = false;
        Map<Integer, Map<String, Object>> toolCallBuffers = new LinkedHashMap<>();

        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.startsWith("data: ")) continue;
            String data = line.substring(6).trim();
            if ("[DONE]".equals(data)) break;

            Map<String, Object> chunk = objectMapper.readValue(data, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
            if (choices == null || choices.isEmpty()) {
                Map<String, Object> usage = (Map<String, Object>) chunk.get("usage");
                if (usage != null) {
                    long input = ((Number) usage.getOrDefault("prompt_tokens", 0)).longValue();
                    long output = ((Number) usage.getOrDefault("completion_tokens", 0)).longValue();
                    events.add(StreamEvent.metadata(input, output, 0));
                }
                continue;
            }

            Map<String, Object> choice = choices.get(0);
            Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
            String finishReason = (String) choice.get("finish_reason");

            if (delta != null) {
                String content = (String) delta.get("content");
                if (content != null) {
                    if (!textStarted) {
                        events.add(StreamEvent.contentBlockStart(contentBlockIndex, Map.of()));
                        textStarted = true;
                    }
                    events.add(StreamEvent.contentBlockDelta(contentBlockIndex, Map.of("text", content)));
                }

                List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) delta.get("tool_calls");
                if (toolCalls != null) {
                    for (Map<String, Object> tc : toolCalls) {
                        int idx = ((Number) tc.get("index")).intValue();
                        toolCallBuffers.computeIfAbsent(idx, k -> new LinkedHashMap<>(Map.of(
                                "id", "", "name", "", "arguments", ""
                        )));
                        Map<String, Object> buf = toolCallBuffers.get(idx);
                        if (tc.get("id") != null) buf.put("id", tc.get("id"));
                        Map<String, Object> fn = (Map<String, Object>) tc.get("function");
                        if (fn != null) {
                            if (fn.get("name") != null) buf.put("name", fn.get("name"));
                            if (fn.get("arguments") != null) {
                                buf.put("arguments", (String) buf.get("arguments") + fn.get("arguments"));
                            }
                        }
                    }
                }
            }

            if (finishReason != null) {
                if (textStarted) {
                    events.add(StreamEvent.contentBlockStop(contentBlockIndex));
                    contentBlockIndex++;
                }

                for (Map.Entry<Integer, Map<String, Object>> entry : toolCallBuffers.entrySet()) {
                    Map<String, Object> buf = entry.getValue();
                    String toolId = (String) buf.get("id");
                    String name = (String) buf.get("name");
                    String args = (String) buf.get("arguments");

                    events.add(StreamEvent.contentBlockStart(contentBlockIndex, Map.of(
                            "toolUse", Map.of("toolUseId", toolId, "name", name)
                    )));
                    events.add(StreamEvent.contentBlockDelta(contentBlockIndex, Map.of(
                            "toolUse", Map.of("input", args)
                    )));
                    events.add(StreamEvent.contentBlockStop(contentBlockIndex));
                    contentBlockIndex++;
                }

                String stopReason = switch (finishReason) {
                    case "tool_calls" -> "tool_use";
                    case "length" -> "max_tokens";
                    default -> "end_turn";
                };
                events.add(StreamEvent.messageStop(stopReason));
            }
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String readStream(HttpURLConnection conn) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception e) {
            return "Unknown error";
        }
    }
}
