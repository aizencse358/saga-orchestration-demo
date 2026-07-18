package com.sagademo.inventory.exception;

public class SimulatedFailureException extends RuntimeException {

    public SimulatedFailureException(String message) {
        super(message);
    }
}
