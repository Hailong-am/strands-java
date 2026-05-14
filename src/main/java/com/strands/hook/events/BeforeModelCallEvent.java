package com.strands.hook.events;

import com.strands.hook.HookEvent;
import com.strands.types.Message;

import java.util.List;

public class BeforeModelCallEvent extends HookEvent {

    private List<Message> messages;
    private int projectedInputTokens;

    public BeforeModelCallEvent(List<Message> messages) {
        this.messages = messages;
    }

    public BeforeModelCallEvent(List<Message> messages, int projectedInputTokens) {
        this.messages = messages;
        this.projectedInputTokens = projectedInputTokens;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public int getProjectedInputTokens() {
        return projectedInputTokens;
    }

    public void setProjectedInputTokens(int projectedInputTokens) {
        this.projectedInputTokens = projectedInputTokens;
    }
}
