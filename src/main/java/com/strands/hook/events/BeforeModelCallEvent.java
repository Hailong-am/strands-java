package com.strands.hook.events;

import com.strands.hook.HookEvent;
import com.strands.types.Message;

import java.util.List;

public class BeforeModelCallEvent extends HookEvent {

    private List<Message> messages;

    public BeforeModelCallEvent(List<Message> messages) {
        this.messages = messages;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
