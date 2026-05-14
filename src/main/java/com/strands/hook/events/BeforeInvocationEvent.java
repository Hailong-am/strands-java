package com.strands.hook.events;

import com.strands.hook.HookEvent;
import com.strands.types.Message;

import java.util.List;

public class BeforeInvocationEvent extends HookEvent {

    private final Object agent;
    private List<Message> messages;
    private String prompt;

    public BeforeInvocationEvent(Object agent, String prompt, List<Message> messages) {
        this.agent = agent;
        this.prompt = prompt;
        this.messages = messages;
    }

    public Object getAgent() {
        return agent;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
