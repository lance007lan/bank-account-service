package com.bank.accountservice.exception;

public class CircuitBreakerOpenException extends RuntimeException {

    public CircuitBreakerOpenException(String circuitName) {
        super("Circuit breaker '" + circuitName + "' is open.");
    }
}
