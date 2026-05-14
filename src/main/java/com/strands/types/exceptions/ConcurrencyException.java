package com.strands.types.exceptions;

public class ConcurrencyException extends RuntimeException {

    public ConcurrencyException(String message) {
        super(message);
    }
}
