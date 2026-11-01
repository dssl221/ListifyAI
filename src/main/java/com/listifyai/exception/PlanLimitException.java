package com.listifyai.exception;

public class PlanLimitException extends RuntimeException {

    public PlanLimitException(String message) {
        super(message);
    }
}
