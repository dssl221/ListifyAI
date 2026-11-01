package com.listifyai.service;

import com.listifyai.exception.ServiceUnavailableException;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
public class ClaudeRateLimiter {

    private static final int MAX_PERMITS = 20;
    private static final long MAX_WAIT_SECONDS = 30;
    private final Semaphore semaphore = new Semaphore(MAX_PERMITS);

    public void acquire() {
        try {
            if (!semaphore.tryAcquire(MAX_WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new ServiceUnavailableException("AI service busy, please retry in a moment.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException("AI service busy, please retry in a moment.", ex);
        }
    }

    public void release() {
        semaphore.release();
    }
}
