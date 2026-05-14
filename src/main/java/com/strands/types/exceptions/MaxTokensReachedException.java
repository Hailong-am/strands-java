package com.strands.types.exceptions;

public class MaxTokensReachedException extends RuntimeException {

    public MaxTokensReachedException(String message) {
        super(message);
    }
}
