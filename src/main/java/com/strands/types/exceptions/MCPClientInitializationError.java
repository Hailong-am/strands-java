package com.strands.types.exceptions;

public class MCPClientInitializationError extends RuntimeException {

    public MCPClientInitializationError(String message) {
        super(message);
    }

    public MCPClientInitializationError(String message, Throwable cause) {
        super(message, cause);
    }
}
