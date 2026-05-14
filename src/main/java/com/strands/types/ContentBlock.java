package com.strands.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContentBlock {

    private String text;
    @JsonProperty("toolUse")
    private ToolUse toolUse;
    @JsonProperty("toolResult")
    private ToolResult toolResult;
    private ImageContent image;
    private DocumentContent document;
    private VideoContent video;
    @JsonProperty("reasoningContent")
    private ReasoningContent reasoningContent;

    public static ContentBlock fromText(String text) {
        ContentBlock block = new ContentBlock();
        block.text = text;
        return block;
    }

    public static ContentBlock fromToolUse(ToolUse toolUse) {
        ContentBlock block = new ContentBlock();
        block.toolUse = toolUse;
        return block;
    }

    public static ContentBlock fromToolResult(ToolResult toolResult) {
        ContentBlock block = new ContentBlock();
        block.toolResult = toolResult;
        return block;
    }

    public static ContentBlock fromImage(ImageContent image) {
        ContentBlock block = new ContentBlock();
        block.image = image;
        return block;
    }

    public static ContentBlock fromDocument(DocumentContent document) {
        ContentBlock block = new ContentBlock();
        block.document = document;
        return block;
    }

    public static ContentBlock fromVideo(VideoContent video) {
        ContentBlock block = new ContentBlock();
        block.video = video;
        return block;
    }

    public static ContentBlock fromReasoning(ReasoningContent reasoning) {
        ContentBlock block = new ContentBlock();
        block.reasoningContent = reasoning;
        return block;
    }

    public boolean isText() {
        return text != null;
    }

    public boolean isToolUse() {
        return toolUse != null;
    }

    public boolean isToolResult() {
        return toolResult != null;
    }
}
