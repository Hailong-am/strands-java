package com.strands.model.gemini;

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

public class GeminiModel implements Model {

    private static final Logger log = LoggerFactory.getLogger(GeminiModel.class);
    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final String DEFAULT_MODEL_ID = "gemini-2.5-flash";

    private final String apiKey;
    private final String baseUrl;
    private final ModelConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiModel(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, DEFAULT_MODEL_ID);
    }

    public GeminiModel(String apiKey, String modelId) {
        this(apiKey, DEFAULT_BASE_URL, modelId);
    }

    public GeminiModel(String apiKey, String baseUrl, String modelId) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.config = new ModelConfig(modelId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<StreamEvent> stream(StreamRequest request) {
        Map<String, Object> body = formatRequest(request);
        List<StreamEvent> events = new ArrayList<>();

        try {
            String url = baseUrl + "/models/" + config.getModelId() + ":streamGenerateContent?alt=sse&key=" + apiKey;
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(objectMapper.writeValueAsBytes(body));
            }

            int status = conn.getResponseCode();
            if (status >= 400) {
                String error = readStream(conn);
                throw new RuntimeException("Gemini API error (" + status + "): " + error);
            }

            events.add(StreamEvent.messageStart("assistant"));
            int contentBlockIndex = 0;
            boolean hasToolCalls = false;
            long totalInputTokens = 0;
            long totalOutputTokens = 0;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) continue;
                    String data = line.substring(6).trim();
                    if (data.isEmpty() || data.equals("[DONE]")) continue;

                    Map<String, Object> chunk = objectMapper.readValue(data, Map.class);
                    List<Map<String, Object>> candidates = (List<Map<String, Object>>) chunk.get("candidates");

                    if (candidates != null && !candidates.isEmpty()) {
                        Map<String, Object> candidate = candidates.get(0);
                        Map<String, Object> content = (Map<String, Object>) candidate.get("content");

                        if (content != null) {
                            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                            if (parts != null) {
                                for (Map<String, Object> part : parts) {
                                    if (part.containsKey("text")) {
                                        String text = (String) part.get("text");
                                        if (contentBlockIndex == 0 || hasToolCalls) {
                                            events.add(StreamEvent.contentBlockStart(contentBlockIndex, Map.of()));
                                        }
                                        events.add(StreamEvent.contentBlockDelta(contentBlockIndex,
                                                Map.of("text", text)));
                                    } else if (part.containsKey("functionCall")) {
                                        Map<String, Object> fc = (Map<String, Object>) part.get("functionCall");
                                        String fcName = (String) fc.get("name");
                                        Map<String, Object> fcArgs = (Map<String, Object>) fc.get("args");
                                        String toolUseId = "tooluse_" + UUID.randomUUID().toString()
                                                .replace("-", "").substring(0, 24);

                                        if (contentBlockIndex > 0 || hasToolCalls) {
                                            events.add(StreamEvent.contentBlockStop(contentBlockIndex));
                                            contentBlockIndex++;
                                        }

                                        events.add(StreamEvent.contentBlockStart(contentBlockIndex, Map.of(
                                                "toolUse", Map.of("toolUseId", toolUseId, "name", fcName))));
                                        events.add(StreamEvent.contentBlockDelta(contentBlockIndex, Map.of(
                                                "toolUse", Map.of("input",
                                                        objectMapper.writeValueAsString(fcArgs != null ? fcArgs : Map.of())))));
                                        events.add(StreamEvent.contentBlockStop(contentBlockIndex));
                                        contentBlockIndex++;
                                        hasToolCalls = true;
                                    }
                                }
                            }
                        }
                    }

                    Map<String, Object> usageMetadata = (Map<String, Object>) chunk.get("usageMetadata");
                    if (usageMetadata != null) {
                        Number promptTokens = (Number) usageMetadata.get("promptTokenCount");
                        Number candidateTokens = (Number) usageMetadata.get("candidatesTokenCount");
                        if (promptTokens != null) totalInputTokens = promptTokens.longValue();
                        if (candidateTokens != null) totalOutputTokens = candidateTokens.longValue();
                    }
                }
            }

            events.add(StreamEvent.contentBlockStop(contentBlockIndex));
            String stopReason = hasToolCalls ? "tool_use" : "end_turn";
            events.add(StreamEvent.messageStop(stopReason));
            events.add(StreamEvent.metadata(totalInputTokens, totalOutputTokens, 0));

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Gemini stream failed", e);
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

        List<Map<String, Object>> contents = new ArrayList<>();

        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", request.getSystemPrompt()))));
        }

        for (Message msg : request.getMessages()) {
            String role = msg.getRole() == Message.Role.USER ? "user" : "model";
            List<Map<String, Object>> parts = new ArrayList<>();

            for (ContentBlock block : msg.getContent()) {
                if (block.isText()) {
                    parts.add(Map.of("text", block.getText()));
                } else if (block.isToolUse()) {
                    ToolUse tu = block.getToolUse();
                    parts.add(Map.of("functionCall", Map.of(
                            "name", tu.getName(),
                            "args", tu.getInput() != null ? tu.getInput() : Map.of()
                    )));
                } else if (block.isToolResult()) {
                    ToolResult tr = block.getToolResult();
                    StringBuilder text = new StringBuilder();
                    if (tr.getContent() != null) {
                        for (ToolResultContent c : tr.getContent()) {
                            if (c.getText() != null) text.append(c.getText());
                        }
                    }
                    parts.add(Map.of("functionResponse", Map.of(
                            "name", tr.getToolUseId(),
                            "response", Map.of("result", text.toString())
                    )));
                }
            }

            contents.add(Map.of("role", role, "parts", parts));
        }

        body.put("contents", contents);

        if (request.getToolSpecs() != null && !request.getToolSpecs().isEmpty()) {
            List<Map<String, Object>> functionDeclarations = new ArrayList<>();
            for (ToolSpec spec : request.getToolSpecs()) {
                Map<String, Object> fd = new LinkedHashMap<>();
                fd.put("name", spec.getName());
                fd.put("description", spec.getDescription() != null ? spec.getDescription() : "");
                if (spec.getInputSchema() != null) {
                    fd.put("parameters", spec.getInputSchema());
                }
                functionDeclarations.add(fd);
            }
            body.put("tools", List.of(Map.of("functionDeclarations", functionDeclarations)));
        }

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        Map<String, Object> params = config.getParameters();
        if (params.containsKey("temperature"))
            generationConfig.put("temperature", params.get("temperature"));
        if (params.containsKey("topP"))
            generationConfig.put("topP", params.get("topP"));
        if (params.containsKey("maxTokens"))
            generationConfig.put("maxOutputTokens", params.get("maxTokens"));
        if (!generationConfig.isEmpty()) {
            body.put("generationConfig", generationConfig);
        }

        return body;
    }

    private String readStream(HttpURLConnection conn) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream(),
                StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String l;
            while ((l = reader.readLine()) != null) sb.append(l);
            return sb.toString();
        } catch (Exception e) {
            return "Unknown error";
        }
    }
}
