package com.strands.model.bedrock;

import com.strands.model.Model;
import com.strands.model.ModelConfig;
import com.strands.model.StreamRequest;
import com.strands.types.ContentBlock;
import com.strands.types.Message;
import com.strands.types.ToolSpec;
import com.strands.types.streaming.StreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class BedrockModel implements Model {

    private static final Logger log = LoggerFactory.getLogger(BedrockModel.class);
    private static final String DEFAULT_MODEL_ID = "us.anthropic.claude-sonnet-4-6-v1";

    private final BedrockRuntimeAsyncClient client;
    private final ModelConfig config;

    public BedrockModel() {
        this(DEFAULT_MODEL_ID);
    }

    public BedrockModel(String modelId) {
        this(BedrockRuntimeAsyncClient.create(), modelId);
    }

    public BedrockModel(BedrockRuntimeAsyncClient client, String modelId) {
        this.client = client;
        this.config = new ModelConfig(modelId);
    }

    @Override
    public Iterator<StreamEvent> stream(StreamRequest request) {
        ConverseStreamRequest converseRequest = buildConverseRequest(request);

        List<StreamEvent> events = Collections.synchronizedList(new ArrayList<>());
        long startTime = System.currentTimeMillis();

        ConverseStreamResponseHandler handler = ConverseStreamResponseHandler.builder()
                .subscriber(ConverseStreamResponseHandler.Visitor.builder()
                        .onMessageStart(e -> events.add(StreamEvent.messageStart("assistant")))
                        .onContentBlockStart(e -> {
                            int idx = e.contentBlockIndex();
                            if (e.start() != null && e.start().toolUse() != null) {
                                events.add(StreamEvent.contentBlockStart(idx, Map.of(
                                        "toolUse", Map.of(
                                                "toolUseId", e.start().toolUse().toolUseId(),
                                                "name", e.start().toolUse().name()
                                        ))));
                            } else {
                                events.add(StreamEvent.contentBlockStart(idx, Map.of()));
                            }
                        })
                        .onContentBlockDelta(e -> {
                            int idx = e.contentBlockIndex();
                            if (e.delta() != null && e.delta().text() != null) {
                                events.add(StreamEvent.contentBlockDelta(idx, Map.of("text", e.delta().text())));
                            } else if (e.delta() != null && e.delta().toolUse() != null) {
                                events.add(StreamEvent.contentBlockDelta(idx, Map.of(
                                        "toolUse", Map.of("input", e.delta().toolUse().input()))));
                            }
                        })
                        .onContentBlockStop(e -> events.add(StreamEvent.contentBlockStop(e.contentBlockIndex())))
                        .onMessageStop(e -> {
                            String reason = e.stopReason() != null ? e.stopReasonAsString() : "end_turn";
                            events.add(StreamEvent.messageStop(reason));
                        })
                        .onMetadata(e -> {
                            long inputTokens = e.usage() != null ? e.usage().inputTokens() : 0;
                            long outputTokens = e.usage() != null ? e.usage().outputTokens() : 0;
                            long latency = e.metrics() != null ? e.metrics().latencyMs() : 0;
                            events.add(StreamEvent.metadata(inputTokens, outputTokens, latency));
                        })
                        .build())
                .onError(e -> log.error("Bedrock stream error", e))
                .build();

        CompletableFuture<Void> future = client.converseStream(converseRequest, handler);
        future.join();

        long totalLatency = System.currentTimeMillis() - startTime;
        boolean hasMetadata = events.stream().anyMatch(e -> e.getType() == StreamEvent.Type.METADATA);
        if (!hasMetadata) {
            events.add(StreamEvent.metadata(0, 0, totalLatency));
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

    private ConverseStreamRequest buildConverseRequest(StreamRequest request) {
        ConverseStreamRequest.Builder builder = ConverseStreamRequest.builder()
                .modelId(config.getModelId());

        List<software.amazon.awssdk.services.bedrockruntime.model.Message> messages = convertMessages(request.getMessages());
        builder.messages(messages);

        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            builder.system(SystemContentBlock.builder().text(request.getSystemPrompt()).build());
        }

        if (request.getToolSpecs() != null && !request.getToolSpecs().isEmpty()) {
            List<Tool> tools = convertToolSpecs(request.getToolSpecs());
            builder.toolConfig(ToolConfiguration.builder().tools(tools).build());
        }

        InferenceConfiguration.Builder inferenceBuilder = InferenceConfiguration.builder();
        Map<String, Object> params = config.getParameters();
        if (params.containsKey("maxTokens")) {
            inferenceBuilder.maxTokens((Integer) params.get("maxTokens"));
        }
        if (params.containsKey("temperature")) {
            inferenceBuilder.temperature(((Number) params.get("temperature")).floatValue());
        }
        if (params.containsKey("topP")) {
            inferenceBuilder.topP(((Number) params.get("topP")).floatValue());
        }
        builder.inferenceConfig(inferenceBuilder.build());

        return builder.build();
    }

    private List<software.amazon.awssdk.services.bedrockruntime.model.Message> convertMessages(List<Message> messages) {
        List<software.amazon.awssdk.services.bedrockruntime.model.Message> result = new ArrayList<>();
        for (Message msg : messages) {
            software.amazon.awssdk.services.bedrockruntime.model.Message.Builder msgBuilder =
                    software.amazon.awssdk.services.bedrockruntime.model.Message.builder();

            msgBuilder.role(msg.getRole() == Message.Role.USER
                    ? ConversationRole.USER : ConversationRole.ASSISTANT);

            List<software.amazon.awssdk.services.bedrockruntime.model.ContentBlock> blocks = new ArrayList<>();
            for (ContentBlock block : msg.getContent()) {
                if (block.isText()) {
                    blocks.add(software.amazon.awssdk.services.bedrockruntime.model.ContentBlock.builder()
                            .text(block.getText()).build());
                } else if (block.isToolUse()) {
                    blocks.add(software.amazon.awssdk.services.bedrockruntime.model.ContentBlock.builder()
                            .toolUse(ToolUseBlock.builder()
                                    .toolUseId(block.getToolUse().getToolUseId())
                                    .name(block.getToolUse().getName())
                                    .input(Document.fromMap(convertToDocumentMap(block.getToolUse().getInput())))
                                    .build())
                            .build());
                } else if (block.isToolResult()) {
                    List<ToolResultContentBlock> resultContent = new ArrayList<>();
                    if (block.getToolResult().getContent() != null) {
                        for (var content : block.getToolResult().getContent()) {
                            if (content.getText() != null) {
                                resultContent.add(ToolResultContentBlock.builder()
                                        .text(content.getText()).build());
                            }
                        }
                    }
                    blocks.add(software.amazon.awssdk.services.bedrockruntime.model.ContentBlock.builder()
                            .toolResult(ToolResultBlock.builder()
                                    .toolUseId(block.getToolResult().getToolUseId())
                                    .status(block.getToolResult().getStatus() == com.strands.types.ToolResult.Status.SUCCESS
                                            ? ToolResultStatus.SUCCESS : ToolResultStatus.ERROR)
                                    .content(resultContent)
                                    .build())
                            .build());
                }
            }
            msgBuilder.content(blocks);
            result.add(msgBuilder.build());
        }
        return result;
    }

    private List<Tool> convertToolSpecs(List<ToolSpec> specs) {
        List<Tool> tools = new ArrayList<>();
        for (ToolSpec spec : specs) {
            tools.add(Tool.builder()
                    .toolSpec(ToolSpecification.builder()
                            .name(spec.getName())
                            .description(spec.getDescription())
                            .inputSchema(ToolInputSchema.builder()
                                    .json(Document.fromMap(convertToDocumentMap(spec.getInputSchema())))
                                    .build())
                            .build())
                    .build());
        }
        return tools;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Document> convertToDocumentMap(Map<String, Object> map) {
        if (map == null) return Map.of();
        Map<String, Document> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result.put(entry.getKey(), toDocument(entry.getValue()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Document toDocument(Object value) {
        if (value == null) return Document.fromNull();
        if (value instanceof String s) return Document.fromString(s);
        if (value instanceof Number n) return Document.fromNumber(n.toString());
        if (value instanceof Boolean b) return Document.fromBoolean(b);
        if (value instanceof Map<?, ?> m) return Document.fromMap(convertToDocumentMap((Map<String, Object>) m));
        if (value instanceof List<?> l) {
            List<Document> docs = new ArrayList<>();
            for (Object item : l) {
                docs.add(toDocument(item));
            }
            return Document.fromList(docs);
        }
        return Document.fromString(value.toString());
    }
}
