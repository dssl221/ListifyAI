package com.listifyai.entity;

public enum ClaudeFailureReason {
    TIMEOUT,
    RATE_LIMITED,
    INVALID_JSON,
    VALIDATION_FAILED,
    UPSTREAM_ERROR,
    COST_LIMIT_EXCEEDED,
    UNKNOWN
}
