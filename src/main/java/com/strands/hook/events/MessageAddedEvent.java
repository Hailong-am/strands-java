package com.strands.hook.events;

import com.strands.hook.HookEvent;
import com.strands.types.Message;

public class MessageAddedEvent extends HookEvent {

    private final Message message;

    public MessageAddedEvent(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
}
