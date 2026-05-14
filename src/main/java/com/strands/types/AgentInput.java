package com.strands.types;

import java.util.List;

/**
 * Represents the diverse input types an agent can accept: a string prompt,
 * a list of content blocks, a full message, or null (continue without user input).
 */
public class AgentInput {

    private final String text;
    private final List<ContentBlock> contentBlocks;
    private final Message message;

    private AgentInput(String text, List<ContentBlock> contentBlocks, Message message) {
        this.text = text;
        this.contentBlocks = contentBlocks;
        this.message = message;
    }

    public static AgentInput fromText(String text) {
        return new AgentInput(text, null, null);
    }

    public static AgentInput fromContent(List<ContentBlock> blocks) {
        return new AgentInput(null, blocks, null);
    }

    public static AgentInput fromMessage(Message message) {
        return new AgentInput(null, null, message);
    }

    public static AgentInput empty() {
        return new AgentInput(null, null, null);
    }

    public boolean isText() {
        return text != null;
    }

    public boolean isContentBlocks() {
        return contentBlocks != null;
    }

    public boolean isMessage() {
        return message != null;
    }

    public boolean isEmpty() {
        return text == null && contentBlocks == null && message == null;
    }

    public String getText() {
        return text;
    }

    public List<ContentBlock> getContentBlocks() {
        return contentBlocks;
    }

    public Message getMessage() {
        return message;
    }

    public Message toMessage() {
        if (message != null) return message;
        if (text != null) return Message.user(text);
        if (contentBlocks != null) return new Message(Message.Role.USER, contentBlocks);
        return null;
    }
}
