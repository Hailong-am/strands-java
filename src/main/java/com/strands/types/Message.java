package com.strands.types;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {

    public enum Role {
        USER("user"),
        ASSISTANT("assistant");

        private final String value;

        Role(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<ContentBlock> getContent() {
        return content;
    }

    public void setContent(List<ContentBlock> content) {
        this.content = content;
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
