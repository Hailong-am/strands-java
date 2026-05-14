package com.strands.model.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strands.model.Model;
import com.strands.model.ModelConfig;
import com.strands.model.StreamRequest;
import com.strands.types.*;
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

/**
 * OpenAI Responses API model. Uses the newer /responses endpoint which supports
 * built-in tools (web_search, code_interpreter, file_search) alongside custom
 * function tools.
 */
public class OpenAIResponsesModel implements Model {

    private static final Logger log = LoggerFactory.getLogger(OpenAIResponsesModel.class);
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final String DEFAULT_MODEL_ID = "gpt-4o";

    private final String apiKey;
    private final String baseUrl;
    private final ModelConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<String> builtInTools = new HashSet<>();

    public OpenAIResponsesModel(String apiKey) {
        this(apiKey, DEFAULT_MODEL_ID);
    }

    public OpenAIResponsesModel(String apiKey, String modelId) {
        this(apiKey, DEFAULT_BASE_URL, modelId);
    }

    public OpenAIResponsesModel(String apiKey, String baseUrl, String modelId) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.config = new ModelConfig(modelId);
    }

    public OpenAIResponsesModel enableWebSearch() {
        builtInTools.add("web_search_preview");
        return this;
    }

    public OpenAIResponsesModel enableCodeInterpreter() {
        builtInTools.add("code_interpreter");
        return this;
    }

    public OpenAIResponsesModel enableFileSearch() {
        builtInTools.add("file_search");
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<StreamEvent> stream(StreamRequest request) {
        Map<String, Object> body = formatRequest(request);
        List<StreamEvent> events = new ArrayList<>();

        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(baseUrl + "/responses")
                    .toURL().openConnection();
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
                throw new RuntimeException("OpenAI Responses API error (" + status + "): " + error);
            }

            long start = System.currentTimeMillis();
            String responseBody = readInputStream(conn);
            long latency = System.currentTimeMillis() - start;

            Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
            events.add(StreamEvent.messageStart("assistant"));

            List<Map<String, Object>> output = (List<Map<String, Object>>) response.get("output");
            int blockIdx = 0;

            if (output != null) {
                for (Map<String, Object> item : output) {
                    String type = (String) item.get("type");
                    if ("message".equals(type)) {
                        List<Map<String, Object>> content = (List<Map<String, Object>>) item.get("content");
                        if (content != null) {
                            for (Map<String, Object> part : content) {
                                String partType = (String) part.get("type");
                                if ("output_text".equals(partType)) {
                                    String text = (String) part.get("text");
                                    events.add(StreamEvent.contentBlockStart(blockIdx, Map.of()));
                                    events.add(StreamEvent.contentBlockDelta(blockIdx, Map.of("text", text)));
                                    events.add(StreamEvent.contentBlockStop(blockIdx));
                                    blockIdx++;
                                }
                            }
                        }
                    } else if ("function_call".equals(type)) {
                        String callId = (String) item.get("call_id");
                        String name = (String) item.get("name");
                        String arguments = (String) item.get("arguments");

                        events.add(StreamEvent.contentBlockStart(blockIdx, Map.of(
                                "toolUse", Map.of("toolUseId", callId, "name", name))));
                        events.add(StreamEvent.contentBlockDelta(blockIdx, Map.of(
                                "toolUse", Map.of("input", arguments != null ? arguments : "{}"))));
                        events.add(StreamEvent.contentBlockStop(blockIdx));
                        blockIdx++;
                    }
                }
            }

            String stopReason = response.containsKey("output") && output != null
                    && output.stream().anyMatch(o -> "function_call".equals(o.get("type")))
                    ? "tool_use" : "end_turn";
            events.add(StreamEvent.messageStop(stopReason));

            Map<String, Object> usage = (Map<String, Object>) response.get("usage");
            long inputTokens = 0, outputTokens = 0;
            if (usage != null) {
                inputTokens = ((Number) usage.getOrDefault("input_tokens", 0)).longValue();
                outputTokens = ((Number) usage.getOrDefault("output_tokens", 0)).longValue();
            }
            events.add(StreamEvent.metadata(inputTokens, outputTokens, latency));

        } catch (ModelThrottledException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("OpenAI Responses stream failed", e);
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

        if (request.getSystemPrompt() != null) {
            body.put("instructions", request.getSystemPrompt());
        }

        StringBuilder inputText = new StringBuilder();
        for (Message msg : request.getMessages()) {
            if (msg.getRole() == Message.Role.USER) {
                inputText.append(msg.getTextContent()).append("\n");
            }
        }
        body.put("input", inputText.toString().trim());

        List<Map<String, Object>> tools = new ArrayList<>();
        for (String builtin : builtInTools) {
            tools.add(Map.of("type", builtin));
        }
        if (request.getToolSpecs() != null) {
            for (ToolSpec spec : request.getToolSpecs()) {
                Map<String, Object> tool = new LinkedHashMap<>();
                tool.put("type", "function");
                tool.put("name", spec.getName());
                tool.put("description", spec.getDescription() != null ? spec.getDescription() : "");
                tool.put("parameters", spec.getInputSchema() != null ? spec.getInputSchema() : Map.of());
                tools.add(tool);
            }
        }
        if (!tools.isEmpty()) {
            body.put("tools", tools);
        }

        return body;
    }

    private String readStream(HttpURLConnection conn) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream(),
                StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception e) {
            return "Unknown error";
        }
    }

    private String readInputStream(HttpURLConnection conn) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read response", e);
        }
    }
}
