package com.strands.types.events;

import java.util.Map;

public abstract class TypedEvent {

    private final String type;
    private final Map<String, Object> data;

    protected TypedEvent(String type, Map<String, Object> data) {
        this.type = type;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public boolean isCallbackEvent() {
        return true;
    }
}
