package com.strands.model.sagemaker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strands.model.Model;
import com.strands.model.ModelConfig;
import com.strands.model.StreamRequest;
import com.strands.types.*;
import com.strands.types.streaming.StreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sagemakerruntime.SageMakerRuntimeClient;
import software.amazon.awssdk.services.sagemakerruntime.model.InvokeEndpointRequest;
import software.amazon.awssdk.services.sagemakerruntime.model.InvokeEndpointResponse;

import java.util.*;

/**
 * Amazon SageMaker endpoint model. Invokes a deployed SageMaker endpoint that
 * serves an OpenAI-compatible chat completions format (e.g., vLLM, TGI, Djl).
 */
public class SageMakerModel implements Model {

    private static final Logger log = LoggerFactory.getLogger(SageMakerModel.class);

    private final SageMakerRuntimeClient client;
    private final String endpointName;
    private final ModelConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SageMakerModel(String endpointName) {
        this(endpointName, Region.US_EAST_1);
    }

    public SageMakerModel(String endpointName, Region region) {
        this.endpointName = endpointName;
        this.config = new ModelConfig(endpointName);
        this.client = SageMakerRuntimeClient.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    public SageMakerModel(String endpointName, SageMakerRuntimeClient client) {
        this.endpointName = endpointName;
        this.config = new ModelConfig(endpointName);
        this.client = client;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<StreamEvent> stream(StreamRequest request) {
        Map<String, Object> body = formatRequest(request);
        List<StreamEvent> events = new ArrayList<>();

        try {
            byte[] payload = objectMapper.writeValueAsBytes(body);

            InvokeEndpointRequest invokeRequest = InvokeEndpointRequest.builder()
                    .endpointName(endpointName)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromByteArray(payload))
                    .build();

            long start = System.currentTimeMillis();
            InvokeEndpointResponse response = client.invokeEndpoint(invokeRequest);
            long latency = System.currentTimeMillis() - start;

            String responseBody = response.body().asUtf8String();
            Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);

            events.add(StreamEvent.messageStart("assistant"));

            List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> choice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) choice.get("message");

                if (message != null) {
                    String content = (String) message.get("content");
                    if (content != null && !content.isEmpty()) {
                        events.add(StreamEvent.contentBlockStart(0, Map.of()));
                        events.add(StreamEvent.contentBlockDelta(0, Map.of("text", content)));
                        events.add(StreamEvent.contentBlockStop(0));
                    }

                    List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
                    if (toolCalls != null) {
                        int idx = content != null ? 1 : 0;
                        for (Map<String, Object> tc : toolCalls) {
                            String id = (String) tc.get("id");
                            Map<String, Object> fn = (Map<String, Object>) tc.get("function");
                            String name = (String) fn.get("name");
                            String args = fn.get("arguments") instanceof String
                                    ? (String) fn.get("arguments")
                                    : objectMapper.writeValueAsString(fn.get("arguments"));

                            events.add(StreamEvent.contentBlockStart(idx, Map.of(
                                    "toolUse", Map.of("toolUseId", id, "name", name))));
                            events.add(StreamEvent.contentBlockDelta(idx, Map.of(
                                    "toolUse", Map.of("input", args))));
                            events.add(StreamEvent.contentBlockStop(idx));
                            idx++;
                        }
                    }

                    String finishReason = (String) choice.get("finish_reason");
                    String stopReason = "tool_calls".equals(finishReason) ? "tool_use" : "end_turn";
                    events.add(StreamEvent.messageStop(stopReason));
                }
            } else {
                events.add(StreamEvent.messageStop("end_turn"));
            }

            Map<String, Object> usage = (Map<String, Object>) result.get("usage");
            long inputTokens = 0, outputTokens = 0;
            if (usage != null) {
                inputTokens = ((Number) usage.getOrDefault("prompt_tokens", 0)).longValue();
                outputTokens = ((Number) usage.getOrDefault("completion_tokens", 0)).longValue();
            }
            events.add(StreamEvent.metadata(inputTokens, outputTokens, latency));

        } catch (Exception e) {
            throw new RuntimeException("SageMaker invocation failed: " + e.getMessage(), e);
        }

        return events.iterator();
    }

    @Override
    public ModelConfig getConfig() {
        return config;
    }

    @Override
    public void updateConfig(Map<String, Object> configUpdates) {
        for (Map.Entry<String, Object> entry : configUpdates.entrySet()) {
            config.setParameter(entry.getKey(), entry.getValue());
        }
    }

    private Map<String, Object> formatRequest(StreamRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelId());

        List<Map<String, Object>> messages = new ArrayList<>();
        if (request.getSystemPrompt() != null) {
            messages.add(Map.of("role", "system", "content", request.getSystemPrompt()));
        }
        for (Message msg : request.getMessages()) {
            String role = msg.getRole() == Message.Role.USER ? "user" : "assistant";
            messages.add(Map.of("role", role, "content", msg.getTextContent()));
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

        Map<String, Object> params = config.getParameters();
        if (params.containsKey("temperature")) body.put("temperature", params.get("temperature"));
        if (params.containsKey("maxTokens")) body.put("max_tokens", params.get("maxTokens"));

        return body;
    }
}
