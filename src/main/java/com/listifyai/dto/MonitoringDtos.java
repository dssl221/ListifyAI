package com.listifyai.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class MonitoringDtos {

    private MonitoringDtos() {}

    public record EnableMonitoringRequest(@NotBlank String url) {}
    public record DisableMonitoringRequest(@NotBlank String listingId) {}

    public record MonitoredListingDTO(
            UUID id,
            String etsyListingId,
            String listingUrl,
            String title,
            String status,
            boolean monitoringEnabled,
            Integer lastScore,
            Instant lastCheckedAt,
            Instant nextCheckAt,
            String optimizationState,
            long openRecommendations
    ) {}

    public record ListingSnapshotDTO(
            UUID id,
            UUID listingId,
            String etsyListingId,
            String title,
            List<String> tags,
            String price,
            String currency,
            int views,
            int favorites,
            int score,
            String checksum,
            Instant capturedAt
    ) {}

    public record MonitoringRecommendationDTO(
            UUID id,
            UUID listingId,
            UUID snapshotId,
            String type,
            String status,
            String severity,
            int opportunityScore,
            String title,
            String message,
            String aiFixText,
            boolean aiCandidate,
            Instant detectedAt
    ) {}

    public record MonitoringJobDTO(
            UUID id,
            UUID listingId,
            String status,
            int attempts,
            Instant availableAt,
            String lastError
    ) {}
}
