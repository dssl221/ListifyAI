package com.listifyai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class EtsyTransientException extends RuntimeException {

    public EtsyTransientException(String message) {
        super(message);
    }

    public EtsyTransientException(String message, Throwable cause) {
        super(message, cause);
    }
}
