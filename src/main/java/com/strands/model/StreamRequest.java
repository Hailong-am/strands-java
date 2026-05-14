package com.strands.model;

import com.strands.tool.ToolChoice;
import com.strands.types.ContentBlock;
import com.strands.types.Message;
import com.strands.types.ToolSpec;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class StreamRequest {

    @Builder.Default
    private final List<Message> messages = List.of();
    @Builder.Default
    private final List<ToolSpec> toolSpecs = List.of();
    private final String systemPrompt;
    private final List<ContentBlock> systemPromptBlocks;
    @Builder.Default
    private final Map<String, Object> modelConfig = Map.of();
    private final ToolChoice toolChoice;

    public String getEffectiveSystemPrompt() {
        if (systemPrompt != null) return systemPrompt;
        if (systemPromptBlocks != null && !systemPromptBlocks.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ContentBlock block : systemPromptBlocks) {
                if (block.isText()) {
                    sb.append(block.getText());
                }
            }
            return sb.toString();
        }
        return null;
    }
}
