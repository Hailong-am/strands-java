package com.strands.handler;

import java.util.Map;

@FunctionalInterface
public interface CallbackHandler {

    void handle(Map<String, Object> kwargs);

    CallbackHandler NULL = kwargs -> {};
}
