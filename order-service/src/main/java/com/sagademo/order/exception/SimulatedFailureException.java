package com.sagademo.order.exception;

public class SimulatedFailureException extends RuntimeException {

    public SimulatedFailureException(String message) {
        super(message);
    }
}
