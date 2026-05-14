package com.strands.model;

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
    @Builder.Default
    private final Map<String, Object> modelConfig = Map.of();
}
