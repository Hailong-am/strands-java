package com.strands.types.events;

import java.util.Map;

public class TextStreamEvent extends TypedEvent {

    private final String delta;

    public TextStreamEvent(String delta) {
        super("text_stream", Map.of("delta", delta));
        this.delta = delta;
    }

    public String getDelta() {
        return delta;
    }
}
