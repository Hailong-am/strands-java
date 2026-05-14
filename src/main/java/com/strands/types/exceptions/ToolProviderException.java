package com.strands.types.exceptions;

public class ToolProviderException extends RuntimeException {

    public ToolProviderException(String message) {
        super(message);
    }

    public ToolProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
