package com.strands.types.events;

import com.strands.types.Message;

import java.util.Map;

public class ModelMessageEvent extends TypedEvent {

    private final Message message;

    public ModelMessageEvent(Message message) {
        super("model_message", Map.of());
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
}
