package com.listifyai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.listifyai.dto.EtsyOutputDTO;
import com.listifyai.dto.ProductInputDTO;
import com.listifyai.dto.ShopifyOutputDTO;
import com.listifyai.dto.TagDTO;
import com.listifyai.entity.ClaudeUsageLog;
import com.listifyai.entity.User;
import com.listifyai.exception.ClaudeApiException;
import com.listifyai.repository.ClaudeUsageLogRepository;
import com.listifyai.security.AuthUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.listifyai.entity.ClaudeFailureReason;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@SuppressWarnings({"null", "unchecked", "unused"})
public class ClaudeService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeService.class);

    // ─── System Prompts ───────────────────────────────────────────────────────

    private static final String ETSY_SYSTEM_PROMPT =
            "You are an expert Etsy SEO copywriter specializing in handmade, vintage, and craft product listings. You understand Etsy's search algorithm deeply.\n" +
            "Output ONLY valid JSON. No preamble, no explanation, no markdown fences.\n" +
            "Etsy rules: Return EXACTLY 3 title variations, never 2 and never 4. Each title must be 120-140 chars, start with the primary keyword, and use comma-separated phrases. Treat Product Name as the primary keyword unless the product details clearly imply a stronger exact keyword. Title 1 angle: keyword-first SEO. Title 2 angle: benefit-led. Title 3 angle: buyer-persona/occasion-led. Description 180-250 words, first 2 sentences shown in search preview so make them vivid and benefit-led, end with Perfect for... line. Tags: EXACTLY 13 tags, each max 20 chars, multi-word buyer-intent phrases, mix broad and specific.\n" +
            "Apply the tone specified by the user.\n" +
            "Tag competition: For each Etsy tag, include competition exactly as \"high\", \"medium\", or \"low\". \"high\" = broad, popular, searched by millions (e.g. \"silver ring\", \"gift for her\"). \"medium\" = category-specific, moderate search volume (e.g. \"copper midi ring\"). \"low\" = long-tail, niche, less competition (e.g. \"hammered copper stacking ring set\"). Mix: aim for 3-4 high, 5-6 medium, 3-4 low across the 13 tags. competition value must be exactly \"high\", \"medium\", or \"low\" - no other values.\n" +
            "Listing Quality Score: Return qualityScore as an integer 0-100. Score using these weights: Title: clear search phrase placement, character usage (25 pts); Description: length adequacy, natural product wording, tone match (25 pts); Tags: count (must be 13), specificity, mix of broad and specific shopper phrases (25 pts); Overall: buyer intent clarity, uniqueness, platform fit (25 pts).\n" +
            "Tips: Return exactly 3 tips, no more no less. Tips must be specific to THIS listing's actual weaknesses, not generic advice. Each tip must be under 100 characters and actionable, e.g. \"Add a size dimension to tag 4\" not \"improve your tags\".\n" +
            "Output format: {\"titles\":[\"variation 1\",\"variation 2\",\"variation 3\"],\"description\":\"string\",\"tags\":[{\"tag\":\"tag1\",\"competition\":\"high\"},{\"tag\":\"tag2\",\"competition\":\"medium\"},{\"tag\":\"tag3\",\"competition\":\"low\"},{\"tag\":\"tag4\",\"competition\":\"high\"},{\"tag\":\"tag5\",\"competition\":\"medium\"},{\"tag\":\"tag6\",\"competition\":\"low\"},{\"tag\":\"tag7\",\"competition\":\"high\"},{\"tag\":\"tag8\",\"competition\":\"medium\"},{\"tag\":\"tag9\",\"competition\":\"low\"},{\"tag\":\"tag10\",\"competition\":\"medium\"},{\"tag\":\"tag11\",\"competition\":\"medium\"},{\"tag\":\"tag12\",\"competition\":\"low\"},{\"tag\":\"tag13\",\"competition\":\"medium\"}],\"qualityScore\":<integer 0-100>,\"tips\":[\"tip 1\",\"tip 2\",\"tip 3\"]}";

    private static final String SHOPIFY_SYSTEM_PROMPT =
            "You are an expert ecommerce copywriter specializing in Shopify product pages and Google Shopping SEO. You write copy that ranks and converts.\n" +
            "Output ONLY valid JSON. No preamble, no explanation, no markdown fences.\n" +
            "Shopify rules: seoTitle 50-60 chars keyword-first. metaDescription EXACTLY 150-160 chars with primary keyword and soft CTA. description 200-300 words in paragraphs benefit-led. bullets EXACTLY 5 items formatted as Feature: Benefit max 12 words each.\n" +
            "Apply the tone specified by the user.\n" +
            "Listing Quality Score: Return qualityScore as an integer 0-100. Score using these weights: Title: clear search phrase placement, character usage (25 pts); Description: length adequacy, natural product wording, tone match (25 pts); Tags: count (must be 13), specificity, mix of broad and specific shopper phrases (25 pts); Overall: buyer intent clarity, uniqueness, platform fit (25 pts).\n" +
            "Tips: Return exactly 3 tips, no more no less. Tips must be specific to THIS listing's actual weaknesses, not generic advice. Each tip must be under 100 characters and actionable, e.g. \"Add a size dimension to tag 4\" not \"improve your tags\".\n" +
            "Output format: {\"seoTitle\":\"string\",\"metaDescription\":\"string\",\"description\":\"string\",\"bullets\":[\"b1\",\"b2\",\"b3\",\"b4\",\"b5\"],\"qualityScore\":<integer 0-100>,\"tips\":[\"tip 1\",\"tip 2\",\"tip 3\"]}";

    private static final String CLAUDE_API_URL    = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_INPUT_CHARS = 40_000;
    private static final int MAX_EXPECTED_OUTPUT_TOKENS = 1500;

    // ─── Dependencies ─────────────────────────────────────────────────────────

    private final HttpClient   httpClient;
    private final ObjectMapper objectMapper;
    private final ClaudeRateLimiter rateLimiter;
    private final AuditService auditService;
    private final ClaudeUsageLogRepository claudeUsageLogRepository;
    private final Executor claudeHttpExecutor;
    private final String       apiKey;
    private final String       model;
    private final int          maxTokens;
    private final MeterRegistry meterRegistry;

    public ClaudeService(
            @Value("${anthropic.api.key}")    String apiKey,
            @Value("${anthropic.model}")      String model,
            @Value("${anthropic.max-tokens}") int    maxTokens,
            ObjectMapper objectMapper,
            ClaudeRateLimiter rateLimiter,
            AuditService auditService,
            ClaudeUsageLogRepository claudeUsageLogRepository,
            @Qualifier("claudeHttpExecutor") Executor claudeHttpExecutor,
            MeterRegistry meterRegistry) {
        this.apiKey             = apiKey;
        this.model              = model;
        this.maxTokens          = maxTokens;
        this.objectMapper       = objectMapper;
        this.rateLimiter        = rateLimiter;
        this.auditService       = auditService;
        this.claudeUsageLogRepository = claudeUsageLogRepository;
        this.claudeHttpExecutor = claudeHttpExecutor;
        this.meterRegistry = meterRegistry;
        this.httpClient   = HttpClient.newBuilder()
                .executor(claudeHttpExecutor)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Generates an Etsy listing asynchronously.
     * Validates that Claude returned exactly 13 tags.
     */
    @Retry(name = "claude")
    @CircuitBreaker(name = "claude")
    public CompletableFuture<EtsyOutputDTO> generateEtsy(ProductInputDTO input) {
        User currentUser = AuthUtils.getCurrentUserOrNull();
        String userMessage = buildUserMessage(input);
        if (userMessage.length() > MAX_INPUT_CHARS) {
            saveClaudeUsage(currentUser, "etsy_generate", "etsy-audit-v1", 0, 0, 0, false, ClaudeFailureReason.VALIDATION_FAILED);
            return CompletableFuture.failedFuture(new ClaudeApiException("Input payload too large. Please shorten the product details."));
        }
        return callClaude(ETSY_SYSTEM_PROMPT, userMessage, "etsy_generate", "etsy-audit-v1", currentUser)
                .thenApply(json -> {
                    try {
                        EtsyOutputDTO dto = objectMapper.readValue(json, EtsyOutputDTO.class);
                        int titleCount = dto.titles() == null ? 0 : dto.titles().size();
                        if (titleCount != 3) {
                            throw new ClaudeApiException("Expected 3 title variations, got " + titleCount);
                        }
                        try {
                            validateEtsyTags(dto.tags());
                            validateQualityFeedback(dto.qualityScore(), dto.tips(), "Etsy");
                        } catch (ClaudeApiException ve) {
                            meterRegistry.counter("listifyai.audit.validation", "status", "failure").increment();
                            throw ve;
                        }
                        return dto;
                    } catch (JsonProcessingException e) {
                        throw new ClaudeApiException("Failed to parse Etsy JSON from Claude: " + e.getMessage(), e);
                    }
                });
    }

    /**
     * Generates a Shopify listing asynchronously.
     * Validates that Claude returned exactly 5 bullets.
     */
    @Retry(name = "claude")
    @CircuitBreaker(name = "claude")
    public CompletableFuture<ShopifyOutputDTO> generateShopify(ProductInputDTO input) {
        User currentUser = AuthUtils.getCurrentUserOrNull();
        String userMessage = buildUserMessage(input);
        if (userMessage.length() > MAX_INPUT_CHARS) {
            saveClaudeUsage(currentUser, "shopify_generate", "shopify-audit-v1", 0, 0, 0, false, ClaudeFailureReason.VALIDATION_FAILED);
            return CompletableFuture.failedFuture(new ClaudeApiException("Input payload too large. Please shorten the product details."));
        }
        return callClaude(SHOPIFY_SYSTEM_PROMPT, userMessage, "shopify_generate", "shopify-audit-v1", currentUser)
                .thenApply(json -> {
                    try {
                        ShopifyOutputDTO dto = objectMapper.readValue(json, ShopifyOutputDTO.class);
                        try {
                            if (dto.bullets() == null || dto.bullets().size() != 5) {
                                throw new ClaudeApiException(
                                        "Claude returned " + (dto.bullets() == null ? 0 : dto.bullets().size())
                                        + " Shopify bullets; expected exactly 5.");
                            }
                            validateQualityFeedback(dto.qualityScore(), dto.tips(), "Shopify");
                        } catch (ClaudeApiException ve) {
                            meterRegistry.counter("listifyai.audit.validation", "status", "failure").increment();
                            throw ve;
                        }
                        return dto;
                    } catch (JsonProcessingException e) {
                        throw new ClaudeApiException("Failed to parse Shopify JSON from Claude: " + e.getMessage(), e);
                    }
                });
    }

    /**
     * Generates both Etsy and Shopify listings IN PARALLEL.
     * Blocks until both complete and returns a {@link GenerationResult}.
     */
    public GenerationResult generateBoth(ProductInputDTO input) {
        CompletableFuture<EtsyOutputDTO>    etsyFuture    = generateEtsy(input);
        CompletableFuture<ShopifyOutputDTO> shopifyFuture = generateShopify(input);

        CompletableFuture.allOf(etsyFuture, shopifyFuture).join();

        return new GenerationResult(etsyFuture.join(), shopifyFuture.join());
    }

    // ─── Inner result type ────────────────────────────────────────────────────

    public record GenerationResult(EtsyOutputDTO etsy, ShopifyOutputDTO shopify) {}

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Async Claude API call using Java 17 {@link HttpClient}.
     * Returns the raw text from content[0].text in Claude's response.
     */
    private CompletableFuture<String> callClaude(String systemPrompt, String userMessage, String callType, String promptVersion, User currentUser) {
        if (apiKey.startsWith("dummy") || apiKey.startsWith("dev-anthropic")) {
            if ("etsy_generate".equals(callType)) {
                return CompletableFuture.completedFuture("{\"titles\":[\"Dummy Title 1\",\"Dummy Title 2\",\"Dummy Title 3\"],\"description\":\"Dummy description.\",\"tags\":[{\"tag\":\"tag1\",\"competition\":\"high\"},{\"tag\":\"tag2\",\"competition\":\"medium\"},{\"tag\":\"tag3\",\"competition\":\"low\"},{\"tag\":\"tag4\",\"competition\":\"high\"},{\"tag\":\"tag5\",\"competition\":\"medium\"},{\"tag\":\"tag6\",\"competition\":\"low\"},{\"tag\":\"tag7\",\"competition\":\"high\"},{\"tag\":\"tag8\",\"competition\":\"medium\"},{\"tag\":\"tag9\",\"competition\":\"low\"},{\"tag\":\"tag10\",\"competition\":\"medium\"},{\"tag\":\"tag11\",\"competition\":\"medium\"},{\"tag\":\"tag12\",\"competition\":\"low\"},{\"tag\":\"tag13\",\"competition\":\"medium\"}],\"qualityScore\":85,\"tips\":[\"Tip 1\",\"Tip 2\",\"Tip 3\"]}");
            } else if ("shopify_generate".equals(callType)) {
                return CompletableFuture.completedFuture("{\"seoTitle\":\"Dummy SEO Title\",\"metaDescription\":\"Dummy meta description with keywords.\",\"description\":\"Dummy description.\",\"bullets\":[\"Bullet 1\",\"Bullet 2\",\"Bullet 3\",\"Bullet 4\",\"Bullet 5\"],\"qualityScore\":90,\"tips\":[\"Tip 1\",\"Tip 2\",\"Tip 3\"]}");
            }
        }
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(Map.of(
                    "model",      model,
                    "max_tokens", maxTokens,
                    "system",     systemPrompt,
                    "messages",   List.of(Map.of("role", "user", "content", userMessage))
            ));
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(
                    new ClaudeApiException("Failed to serialize Claude request body", e));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CLAUDE_API_URL))
                .timeout(Duration.ofSeconds(45))
                .header("x-api-key",          apiKey)
                .header("anthropic-version",   ANTHROPIC_VERSION)
                .header("Content-Type",        "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        rateLimiter.acquire();
        auditService.log(null, "claude.call", "Claude", model, null, Map.of("maxTokens", maxTokens));
        long startTime = System.currentTimeMillis();
        try {
            return httpClient
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        long latencyMs = System.currentTimeMillis() - startTime;
                        if (response.statusCode() != 200) {
                            ClaudeFailureReason reason = response.statusCode() == 429 ? ClaudeFailureReason.RATE_LIMITED : ClaudeFailureReason.UPSTREAM_ERROR;
                            saveClaudeUsage(currentUser, callType, promptVersion, 0, 0, (int) latencyMs, false, reason);
                            throw new ClaudeApiException(
                                    "Claude API returned HTTP " + response.statusCode()
                                    + ": " + response.body());
                        }
                        try {
                            JsonNode root = objectMapper.readTree(response.body());
                            try {
                                int inputTokens = root.path("usage")
                                        .path("input_tokens").asInt(0);
                                int outputTokens = root.path("usage")
                                        .path("output_tokens").asInt(0);
                                        
                                if (outputTokens > MAX_EXPECTED_OUTPUT_TOKENS) {
                                    log.warn("Anomaly: Claude output tokens exceeded expected max. Output Tokens: {}", outputTokens);
                                }

                                saveClaudeUsage(currentUser, callType, promptVersion, inputTokens, outputTokens, (int) latencyMs, true, null);
                            } catch (Exception logEx) {
                                log.warn("Failed to save Claude usage log: {}", logEx.getMessage());
                            }
                            return extractTextContent(root);
                        } catch (JsonProcessingException ex) {
                            saveClaudeUsage(currentUser, callType, promptVersion, 0, 0, (int) latencyMs, false, ClaudeFailureReason.INVALID_JSON);
                            throw new ClaudeApiException("Failed to parse Claude response: " + ex.getMessage(), ex);
                        }
                    })
                    .whenComplete((ignored, throwable) -> rateLimiter.release());
        } catch (RuntimeException ex) {
            rateLimiter.release();
            ClaudeFailureReason reason = ClaudeFailureReason.UNKNOWN;
            if (ex.getCause() instanceof HttpTimeoutException || (ex.getCause() != null && ex.getCause().getCause() instanceof HttpTimeoutException)) {
                reason = ClaudeFailureReason.TIMEOUT;
            }
            saveClaudeUsage(currentUser, callType, promptVersion, 0, 0, (int) (System.currentTimeMillis() - startTime), false, reason);
            return CompletableFuture.failedFuture(ex);
        }
    }

    /**
     * Extracts {@code content[0].text} from Anthropic's response envelope,
     * stripping any accidental markdown code fences Claude may include.
     */
    private String extractTextContent(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return extractTextContent(root);
        } catch (Exception e) {
            throw new ClaudeApiException(
                    "Failed to extract text from Claude response: " + e.getMessage(), e);
        }
    }

    private String extractTextContent(JsonNode root) {
        try {
            String   text = root.path("content").get(0).path("text").asText();
            // Strip ```json ... ``` fences in case Claude wraps output
            return text.replaceAll("(?s)```json\\s*|```", "").trim();
        } catch (Exception e) {
            throw new ClaudeApiException(
                    "Failed to extract text from Claude response: " + e.getMessage(), e);
        }
    }

    private void saveClaudeUsage(User user, String requestType, String promptVersion, int inputTokens, int outputTokens, int latencyMs, boolean success, ClaudeFailureReason failureReason) {
        
        meterRegistry.timer("listifyai.claude.duration").record(Duration.ofMillis(latencyMs));
        meterRegistry.counter("listifyai.claude.request", "status", success ? "success" : "failure").increment();
        if (inputTokens > 0) {
            meterRegistry.counter("listifyai.claude.tokens", "type", "input").increment(inputTokens);
        }
        if (outputTokens > 0) {
            meterRegistry.counter("listifyai.claude.tokens", "type", "output").increment(outputTokens);
        }

        try {
            ClaudeUsageLog log = new ClaudeUsageLog();
            log.setUser(user);
            log.setCallType(requestType);
            log.setRequestType(requestType);
            log.setPromptVersion(promptVersion);
            log.setInputTokens(inputTokens);
            log.setOutputTokens(outputTokens);
            log.setModel(model);
            log.setLatencyMs(latencyMs);
            log.setSuccess(success);
            log.setFailureReason(failureReason);
            // simple cost estimation logic based on sonnet pricing per 1M tokens ($3 input, $15 output)
            if (inputTokens > 0 || outputTokens > 0) {
                BigDecimal inputCost = BigDecimal.valueOf(inputTokens).multiply(BigDecimal.valueOf(0.000003));
                BigDecimal outputCost = BigDecimal.valueOf(outputTokens).multiply(BigDecimal.valueOf(0.000015));
                log.setEstimatedCost(inputCost.add(outputCost));
            }
            claudeUsageLogRepository.save(log);
        } catch (Exception ignored) {
            log.warn("Failed to save Claude usage log: {}", ignored.getMessage());
        }
    }

    /** Formats all ProductInputDTO fields into a clear user message for Claude. */
    private String buildUserMessage(ProductInputDTO input) {
        return String.format("""
                Product Name   : %s
                Material       : %s
                Size           : %s
                Target Buyer   : %s
                Use Case       : %s
                Occasion       : %s
                Tone           : %s
                Platform       : %s
                """,
                nullSafe(input.productName()),
                nullSafe(input.material()),
                nullSafe(input.size()),
                nullSafe(input.targetBuyer()),
                nullSafe(input.useCase()),
                nullSafe(input.occasion()),
                nullSafe(input.tone()),
                nullSafe(input.platform()));
    }

    private String nullSafe(String value) {
        return value != null ? value : "N/A";
    }

    private void validateQualityFeedback(int qualityScore, List<String> tips, String platform) {
        if (qualityScore < 0 || qualityScore > 100) {
            throw new ClaudeApiException(platform + " qualityScore must be between 0 and 100, got " + qualityScore);
        }
        int tipCount = tips == null ? 0 : tips.size();
        if (tipCount != 3) {
            throw new ClaudeApiException(platform + " tips must contain exactly 3 items, got " + tipCount);
        }
    }

    private void validateEtsyTags(List<TagDTO> tags) {
        if (tags == null || tags.size() != 13) {
            throw new ClaudeApiException(
                    "Claude returned " + (tags == null ? 0 : tags.size())
                            + " Etsy tags; expected exactly 13.");
        }

        for (int i = 0; i < tags.size(); i++) {
            TagDTO tag = tags.get(i);
            int position = i + 1;
            if (tag == null) {
                throw new ClaudeApiException("Etsy tag " + position + " is null.");
            }
            if (tag.tag() == null || tag.tag().isBlank()) {
                throw new ClaudeApiException("Etsy tag " + position + " is missing tag text.");
            }
            if (tag.tag().length() > 20) {
                throw new ClaudeApiException(
                        "Etsy tag " + position + " exceeds 20 characters: " + tag.tag());
            }
            if (!isValidCompetition(tag.competition())) {
                throw new ClaudeApiException(
                        "Etsy tag " + position + " has invalid competition \""
                                + tag.competition() + "\"; expected high, medium, or low.");
            }
        }
    }

    private boolean isValidCompetition(String competition) {
        return "high".equals(competition) || "medium".equals(competition) || "low".equals(competition);
    }
}
