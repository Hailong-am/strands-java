package com.strands.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContentBlock {

    private String text;
    private ToolUse toolUse;
    private ToolResult toolResult;
    private ImageContent image;
    private DocumentContent document;
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

    public static ContentBlock fromReasoning(ReasoningContent reasoning) {
        ContentBlock block = new ContentBlock();
        block.reasoningContent = reasoning;
        return block;
    }

    public String getText() {
        return text;
    }

    @JsonProperty("toolUse")
    public ToolUse getToolUse() {
        return toolUse;
    }

    @JsonProperty("toolResult")
    public ToolResult getToolResult() {
        return toolResult;
    }

    public ImageContent getImage() {
        return image;
    }

    public DocumentContent getDocument() {
        return document;
    }

    @JsonProperty("reasoningContent")
    public ReasoningContent getReasoningContent() {
        return reasoningContent;
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
