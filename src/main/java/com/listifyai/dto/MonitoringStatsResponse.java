package com.listifyai.dto;

import java.time.OffsetDateTime;

public record MonitoringStatsResponse(
        long queueDepth,
        long deadLetterCount,
        long processingCount,
        double avgProcessingMs,
        OffsetDateTime lastCompletedAt,
        long totalEventsToday
) {}
