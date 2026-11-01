package com.listifyai.monitoring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.listifyai.entity.ClaudeUsageLog;
import com.listifyai.entity.ListingSnapshot;
import com.listifyai.entity.MonitoringRecommendation;
import com.listifyai.entity.User;
import com.listifyai.exception.ClaudeApiException;
import com.listifyai.monitoring.MonitoringEnums.MonitoringRecommendationStatus;
import com.listifyai.repository.ClaudeUsageLogRepository;
import com.listifyai.repository.ListingSnapshotRepository;
import com.listifyai.repository.MonitoringRecommendationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@SuppressWarnings({"null", "unchecked", "unused"})
public class MonitoringAiFixService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringAiFixService.class);

    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private static final String SYSTEM_PROMPT = """
            You are Tagwing, an Etsy listing advisor.
            A background monitor has detected a specific SEO issue in a seller's live Etsy listing.

            Your job: generate ONE precise, copy-paste-ready fix for the exact issue detected.

            Rules:
            - Fix must reference actual content from the listing (real tags, real title words, real product type)
            - Maximum 3 sentences total
            - Plain English - no SEO jargon, no "keyword density", no "semantic indexing"
            - Be specific: name the exact tag to replace, the exact phrase to add, the exact character count to reach
            - Start with an action verb: "Replace...", "Add...", "Change...", "Move..."
            - Output ONLY the fix text - no preamble, no JSON, no labels, no quotes
            """;

    private final MonitoringRecommendationRepository recommendationRepository;
    private final ListingSnapshotRepository snapshotRepository;
    private final ClaudeUsageLogRepository claudeUsageLogRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;
    private MonitoringAiFixService self;

    public MonitoringAiFixService(MonitoringRecommendationRepository recommendationRepository,
                                  ListingSnapshotRepository snapshotRepository,
                                  ClaudeUsageLogRepository claudeUsageLogRepository,
                                  ObjectMapper objectMapper,
                                  @Value("${anthropic.api.key}") String apiKey,
                                  @Value("${anthropic.model}") String model) {
        this.recommendationRepository = recommendationRepository;
        this.snapshotRepository = snapshotRepository;
        this.claudeUsageLogRepository = claudeUsageLogRepository;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Autowired
    public void setSelf(@Lazy MonitoringAiFixService self) {
        this.self = self;
    }

    @Async("monitoringAiFixExecutor")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void processPendingAiFixes(int batchSize) {
        List<UUID> pendingIds = recommendationRepository
                .findByAiCandidateTrueAndStatusOrderByOpportunityScoreDesc(
                        MonitoringRecommendationStatus.PENDING_AI,
                        PageRequest.of(0, batchSize)
                )
                .stream()
                .map(MonitoringRecommendation::getId)
                .toList();

        for (UUID recommendationId : pendingIds) {
            try {
                self.generateAndSaveFix(recommendationId);
            } catch (Exception ex) {
                log.warn("AI fix generation failed for recommendation {}: {}",
                        recommendationId, ex.getMessage());
            }
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public MonitoringRecommendation generateFixForRecommendation(UUID recommendationId, UUID userId) {
        return self.generateAndSaveFix(recommendationId, userId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MonitoringRecommendation generateAndSaveFix(UUID recommendationId) {
        MonitoringRecommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found."));

        return generateAndSaveFix(recommendation);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MonitoringRecommendation generateAndSaveFix(UUID recommendationId, UUID userId) {
        MonitoringRecommendation recommendation = recommendationRepository.findByIdAndUserId(recommendationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found."));

        return generateAndSaveFix(recommendation);
    }

    private MonitoringRecommendation generateAndSaveFix(MonitoringRecommendation recommendation) {
        ListingSnapshot snapshot = snapshotRepository
                .findFirstByMonitoredListingIdOrderByCapturedAtDesc(recommendation.getMonitoredListing().getId())
                .orElse(null);

        String userMessage = buildUserMessage(recommendation, snapshot);
        String aiFixText = callClaude(userMessage, recommendation.getUser());

        recommendation.setAiFixText(aiFixText);
        recommendation.setAiGeneratedAt(OffsetDateTime.now());
        recommendation.setStatus(MonitoringRecommendationStatus.OPEN);
        return recommendationRepository.save(recommendation);
    }

    private String buildUserMessage(MonitoringRecommendation recommendation, ListingSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        sb.append("Issue type: ").append(recommendation.getRecommendationType()).append("\n");
        sb.append("Issue detected: ").append(recommendation.getMessage()).append("\n");

        if (snapshot != null) {
            sb.append("Listing title: ").append(safe(snapshot.getTitle())).append("\n");

            if (snapshot.getTags() != null && !snapshot.getTags().isBlank()) {
                try {
                    List<?> tags = objectMapper.readValue(snapshot.getTags(), List.class);
                    sb.append("Current tags: ")
                            .append(String.join(", ", tags.stream().map(Object::toString).toList()))
                            .append("\n");
                } catch (Exception ignored) {
                    // Missing tag context should not block a useful fix.
                }
            }

            sb.append("Current score: ").append(snapshot.getScore()).append("/100\n");

            if (snapshot.getDescription() != null && !snapshot.getDescription().isBlank()) {
                String description = snapshot.getDescription().length() > 400
                        ? snapshot.getDescription().substring(0, 400) + "..."
                        : snapshot.getDescription();
                sb.append("Description start: ").append(description).append("\n");
            }
        }

        if (recommendation.getDetails() != null && !recommendation.getDetails().isBlank()) {
            try {
                JsonNode details = objectMapper.readTree(recommendation.getDetails());
                sb.append("Rule details: ").append(details.toPrettyString()).append("\n");
            } catch (Exception ignored) {
                // Rule details are supplemental.
            }
        }

        sb.append("\nGenerate a specific, actionable fix for this issue.");
        return sb.toString();
    }

    private String callClaude(String userMessage, User user) {
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "max_tokens", 300,
                    "system", SYSTEM_PROMPT,
                    "messages", List.of(Map.of("role", "user", "content", userMessage))
            ));
        } catch (JsonProcessingException ex) {
            throw new ClaudeApiException("Failed to serialize AI fix request", ex);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CLAUDE_API_URL))
                .timeout(Duration.ofSeconds(30))
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        long startedAt = System.nanoTime();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int latencyMs = (int) Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            if (response.statusCode() != 200) {
                saveClaudeUsage(user, 0, 0, latencyMs, false);
                throw new ClaudeApiException("Claude API returned HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            saveClaudeUsage(
                    user,
                    root.path("usage").path("input_tokens").asInt(0),
                    root.path("usage").path("output_tokens").asInt(0),
                    latencyMs,
                    true
            );
            return root.path("content").get(0).path("text").asText().trim();
        } catch (ClaudeApiException ex) {
            throw ex;
        } catch (Exception ex) {
            saveClaudeUsage(user, 0, 0, (int) Duration.ofNanos(System.nanoTime() - startedAt).toMillis(), false);
            throw new ClaudeApiException("AI fix request failed: " + ex.getMessage(), ex);
        }
    }

    private void saveClaudeUsage(User user, int inputTokens, int outputTokens, int latencyMs, boolean success) {
        try {
            ClaudeUsageLog log = new ClaudeUsageLog();
            log.setUser(user);
            log.setCallType("ai_fix");
            log.setInputTokens(inputTokens);
            log.setOutputTokens(outputTokens);
            log.setModel(model);
            log.setLatencyMs(latencyMs);
            log.setSuccess(success);
            claudeUsageLogRepository.save(log);
        } catch (Exception ignored) {
            // Usage logging must not block monitoring fixes.
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
