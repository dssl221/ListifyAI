package com.listifyai.monitoring;

public final class MonitoringEnums {

    private MonitoringEnums() {}

    public enum MonitoredListingStatus {
        ACTIVE,
        PAUSED,
        ERROR
    }

    public enum MonitoringJobStatus {
        PENDING,
        RUNNING,
        PROCESSING,
        COMPLETED,
        FAILED,
        RETRYING,
        DEAD_LETTER
    }

    public enum MonitoringJobType {
        LISTING_CHECK
    }

    public enum MonitoringSeverity {
        INFO,
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum MonitoringRecommendationStatus {
        OPEN,
        DISMISSED,
        RESOLVED,
        PENDING_AI
    }

    public enum OptimizationState {
        NEVER_OPTIMIZED,
        OPTIMIZED,
        STALE,
        NEEDS_REVIEW
    }
}
