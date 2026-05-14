package com.strands.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {

    public enum Role {
        USER("user"),
        ASSISTANT("assistant");

        @Getter
        private final String value;

        Role(String value) {
            this.value = value;
        }
    }

    private Role role;
    private List<ContentBlock> content;

    public Message() {
        this.content = new ArrayList<>();
    }

    public Message(Role role, List<ContentBlock> content) {
        this.role = role;
        this.content = content != null ? new ArrayList<>(content) : new ArrayList<>();
    }

    public static Message user(String text) {
        return new Message(Role.USER, List.of(ContentBlock.fromText(text)));
    }

    public static Message assistant(String text) {
        return new Message(Role.ASSISTANT, List.of(ContentBlock.fromText(text)));
    }

    public static Message toolResult(ToolResult... results) {
        List<ContentBlock> blocks = new ArrayList<>();
        for (ToolResult result : results) {
            blocks.add(ContentBlock.fromToolResult(result));
        }
        return new Message(Role.USER, blocks);
    }

    public String getTextContent() {
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : content) {
            if (block.isText()) {
                sb.append(block.getText());
            }
        }
        return sb.toString();
    }

    public List<ToolUse> getToolUses() {
        List<ToolUse> toolUses = new ArrayList<>();
        for (ContentBlock block : content) {
            if (block.isToolUse()) {
                toolUses.add(block.getToolUse());
            }
        }
        return toolUses;
    }

    public boolean hasToolUse() {
        return content.stream().anyMatch(ContentBlock::isToolUse);
    }
}
