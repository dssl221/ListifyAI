package com.listifyai.service;
public class EmailDeliveryException extends RuntimeException {
    private final boolean retryable;
    public EmailDeliveryException(String message, boolean retryable, Throwable cause) { super(message, cause); this.retryable = retryable; }
    public boolean isRetryable() { return retryable; }
}
