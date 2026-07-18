package com.sagademo.shipping.exception;

public class SimulatedFailureException extends RuntimeException {

    public SimulatedFailureException(String message) {
        super(message);
    }
}
