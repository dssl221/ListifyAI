package com.listifyai.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EtsyAuditDtos {

    private EtsyAuditDtos() {}

    public record AnalyzeRequest(@NotBlank String url) {}

    public record RegenerateRequest(@NotBlank String analysisId) {}

    public record AuditUsageDTO(
            String plan,
            int used,
            int limit,
            int remaining,
            LocalDate resetDate
    ) {}

    public record HistoryItemDTO(
            UUID analysisId,
            String listingId,
            String listingUrl,
            String title,
            int score,
            Instant createdAt
    ) {}

    public record AnalyzedListingDTO(
            UUID id,
            String etsyListingId,
            String listingUrl,
            String title,
            List<String> tags,
            String description,
            Instant fetchedAt
    ) {}

    public record ListingAuditDTO(
            UUID analysisId,
            boolean cached,
            AnalyzedListingDTO listing,
            AuditUsageDTO usage,
            AuditResultDTO audit
    ) {}

    public record SectionRegenerationDTO(
            UUID analysisId,
            String section,
            Object value,
            AuditResultDTO audit
    ) {}

    public record AuditResultDTO(
            int score,
            String scoreExplanation,
            String summary,
            SectionInsightDTO weakTagAnalysis,
            SectionInsightDTO titleClarityAnalysis,
            SectionInsightDTO buyerIntentAnalysis,
            SectionInsightDTO personalizationAnalysis,
            SectionInsightDTO giftingIntentAnalysis,
            SectionInsightDTO readabilityReview,
            List<String> actionableFixes,
            String optimizedTitle,
            List<String> optimizedTags,
            String optimizedDescription
    ) {}

    public record SectionInsightDTO(
            String status,
            String message,
            String fix
    ) {}

    public record CompactEtsyListing(
            String listingId,
            String url,
            String title,
            String description,
            List<String> tags,
            String taxonomyPath,
            String whoMade,
            String whenMade,
            String isSupply,
            String price,
            String currency,
            Map<String, Object> signals
    ) {}
}
