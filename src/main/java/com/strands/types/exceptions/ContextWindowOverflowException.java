package com.strands.types.exceptions;

public class ContextWindowOverflowException extends RuntimeException {

    public ContextWindowOverflowException() {
        super("Context window overflow");
    }

    public ContextWindowOverflowException(String message) {
        super(message);
    }

    public ContextWindowOverflowException(String message, Throwable cause) {
        super(message, cause);
    }
}
