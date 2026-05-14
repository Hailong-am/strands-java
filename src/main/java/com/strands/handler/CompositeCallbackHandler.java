package com.strands.handler;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CompositeCallbackHandler implements CallbackHandler {

    private final List<CallbackHandler> handlers;

    public CompositeCallbackHandler(CallbackHandler... handlers) {
        this.handlers = Arrays.asList(handlers);
    }

    @Override
    public void handle(Map<String, Object> kwargs) {
        for (CallbackHandler handler : handlers) {
            handler.handle(kwargs);
        }
    }
}
