package com.listifyai.exception;

public class ListingLimitException extends RuntimeException {

    public ListingLimitException(String message) {
        super(message);
    }

    public ListingLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
