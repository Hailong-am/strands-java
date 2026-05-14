package com.strands.model.ollama;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strands.model.Model;
import com.strands.model.ModelConfig;
import com.strands.model.StreamRequest;
import com.strands.types.*;
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

public class OllamaModel implements Model {

    private static final Logger log = LoggerFactory.getLogger(OllamaModel.class);
    private static final String DEFAULT_HOST = "http://localhost:11434";
    private static final String DEFAULT_MODEL_ID = "llama3.1";

    private final String host;
    private final ModelConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaModel() {
        this(DEFAULT_HOST, DEFAULT_MODEL_ID);
    }

    public OllamaModel(String host, String modelId) {
        this.host = host != null ? host : DEFAULT_HOST;
        this.config = new ModelConfig(modelId);
    }

    @Override
    public Iterator<StreamEvent> stream(StreamRequest request) {
        Map<String, Object> body = formatRequest(request);
        List<StreamEvent> events = new ArrayList<>();

        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(host + "/api/chat").toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(objectMapper.writeValueAsBytes(body));
            }

            int status = conn.getResponseCode();
            if (status >= 400) {
                String error = readStream(conn);
                throw new RuntimeException("Ollama API error (" + status + "): " + error);
            }

            events.add(StreamEvent.messageStart("assistant"));
            int contentBlockIndex = 0;
            events.add(StreamEvent.contentBlockStart(contentBlockIndex, Map.of()));
            boolean toolRequested = false;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;

                    @SuppressWarnings("unchecked")
                    Map<String, Object> chunk = objectMapper.readValue(line, Map.class);

                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) chunk.get("message");
                    if (message != null) {
                        String content = (String) message.get("content");
                        if (content != null && !content.isEmpty()) {
                            events.add(StreamEvent.contentBlockDelta(contentBlockIndex, Map.of("text", content)));
                        }

                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
                        if (toolCalls != null && !toolCalls.isEmpty()) {
                            events.add(StreamEvent.contentBlockStop(contentBlockIndex));
                            contentBlockIndex++;

                            for (Map<String, Object> tc : toolCalls) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> fn = (Map<String, Object>) tc.get("function");
                                String name = (String) fn.get("name");
                                @SuppressWarnings("unchecked")
                                Map<String, Object> args = (Map<String, Object>) fn.get("arguments");
                                String toolUseId = "tooluse_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

                                events.add(StreamEvent.contentBlockStart(contentBlockIndex, Map.of(
                                        "toolUse", Map.of("toolUseId", toolUseId, "name", name))));
                                events.add(StreamEvent.contentBlockDelta(contentBlockIndex, Map.of(
                                        "toolUse", Map.of("input", objectMapper.writeValueAsString(args)))));
                                events.add(StreamEvent.contentBlockStop(contentBlockIndex));
                                contentBlockIndex++;
                                toolRequested = true;
                            }

                            events.add(StreamEvent.contentBlockStart(contentBlockIndex, Map.of()));
                        }
                    }

                    Boolean done = (Boolean) chunk.get("done");
                    if (Boolean.TRUE.equals(done)) {
                        events.add(StreamEvent.contentBlockStop(contentBlockIndex));

                        String stopReason = toolRequested ? "tool_use" : "end_turn";
                        events.add(StreamEvent.messageStop(stopReason));

                        Number totalDuration = (Number) chunk.get("total_duration");
                        long latencyMs = totalDuration != null ? totalDuration.longValue() / 1_000_000 : 0;
                        Number promptEvalCount = (Number) chunk.get("prompt_eval_count");
                        Number evalCount = (Number) chunk.get("eval_count");
                        long input = promptEvalCount != null ? promptEvalCount.longValue() : 0;
                        long output = evalCount != null ? evalCount.longValue() : 0;
                        events.add(StreamEvent.metadata(input, output, latencyMs));
                    }
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Ollama stream failed", e);
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

        List<Map<String, Object>> messages = new ArrayList<>();

        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            messages.add(Map.of("role", "system", "content", request.getSystemPrompt()));
        }

        for (Message msg : request.getMessages()) {
            String role = msg.getRole() == Message.Role.USER ? "user" : "assistant";

            for (ContentBlock block : msg.getContent()) {
                if (block.isText()) {
                    messages.add(Map.of("role", role, "content", block.getText()));
                } else if (block.isToolResult()) {
                    ToolResult tr = block.getToolResult();
                    StringBuilder text = new StringBuilder();
                    if (tr.getContent() != null) {
                        for (ToolResultContent c : tr.getContent()) {
                            if (c.getText() != null) text.append(c.getText());
                        }
                    }
                    messages.add(Map.of("role", "tool", "content", text.toString()));
                }
            }
        }

        body.put("messages", messages);

        if (request.getToolSpecs() != null && !request.getToolSpecs().isEmpty()) {
            List<Map<String, Object>> tools = new ArrayList<>();
            for (ToolSpec spec : request.getToolSpecs()) {
                tools.add(Map.of(
                        "type", "function",
                        "function", Map.of(
                                "name", spec.getName(),
                                "description", spec.getDescription() != null ? spec.getDescription() : "",
                                "parameters", spec.getInputSchema() != null ? spec.getInputSchema() : Map.of()
                        )
                ));
            }
            body.put("tools", tools);
        }

        Map<String, Object> options = new LinkedHashMap<>();
        Map<String, Object> params = config.getParameters();
        if (params.containsKey("temperature")) options.put("temperature", params.get("temperature"));
        if (params.containsKey("topP")) options.put("top_p", params.get("topP"));
        if (params.containsKey("maxTokens")) options.put("num_predict", params.get("maxTokens"));
        if (!options.isEmpty()) body.put("options", options);

        return body;
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
