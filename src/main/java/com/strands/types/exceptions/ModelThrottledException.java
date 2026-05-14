package com.strands.types.exceptions;

public class ModelThrottledException extends RuntimeException {

    public ModelThrottledException(String message) {
        super(message);
    }

    public ModelThrottledException(String message, Throwable cause) {
        super(message, cause);
    }
}
