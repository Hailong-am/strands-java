package com.strands.types.exceptions;

import java.util.Map;

public class EventLoopException extends RuntimeException {

    private final Exception originalException;
    private final Map<String, Object> requestState;

    public EventLoopException(Exception originalException, Map<String, Object> requestState) {
        super(originalException.getMessage(), originalException);
        this.originalException = originalException;
        this.requestState = requestState;
    }

    public Exception getOriginalException() {
        return originalException;
    }

    public Map<String, Object> getRequestState() {
        return requestState;
    }
}
