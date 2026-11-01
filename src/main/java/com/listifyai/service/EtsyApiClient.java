package com.listifyai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.listifyai.dto.EtsyAuditDtos.CompactEtsyListing;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.listifyai.exception.EtsyTransientException;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EtsyApiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String accessToken;
    private final MeterRegistry meterRegistry;

    public EtsyApiClient(ObjectMapper objectMapper,
            @Value("${etsy.api.key:}") String apiKey,
            @Value("${etsy.access.token:}") String accessToken,
            MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.accessToken = accessToken;
        this.meterRegistry = meterRegistry;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Retry(name = "etsyApi")
    @CircuitBreaker(name = "etsyApi", fallbackMethod = "etsyFallback")
    public CompactEtsyListing fetchListing(String listingId, String canonicalUrl) {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("dummy")) {
            return new CompactEtsyListing(
                    listingId,
                    canonicalUrl,
                    "Mocked Ceramic Mug",
                    "A beautiful handcrafted ceramic mug.",
                    java.util.List.of("mug", "ceramic", "handmade"),
                    "Home & Living > Kitchen & Dining",
                    "someone_else",
                    "made_to_order",
                    "false",
                    "25.00",
                    "USD",
                    java.util.Map.of("views", 100, "numFavorers", 5, "state", "active", "quantity", 10, "hasVariations", false)
            );
        }

        URI uri = URI.create("https://api.etsy.com/v3/application/listings/" + listingId);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(30))
                .header("x-api-key", apiKey)
                .GET();

        if (accessToken != null && !accessToken.isBlank()) {
            builder.header("Authorization", "Bearer " + accessToken);
        }

        long startedAt = System.nanoTime();
        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            int latencyMs = (int) Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            meterRegistry.timer("listifyai.etsy.duration").record(Duration.ofMillis(latencyMs));

            if (response.statusCode() == 404) {
                meterRegistry.counter("listifyai.etsy.error", "type", "not_found").increment();
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Etsy listing not found.");
            }
            if (response.statusCode() == 429) {
                meterRegistry.counter("listifyai.etsy.error", "type", "rate_limited").increment();
                throw new EtsyTransientException("Etsy is temporarily rate-limiting requests. Please try again later.");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                meterRegistry.counter("listifyai.etsy.error", "type", "upstream_error").increment();
                throw new EtsyTransientException("Could not fetch Etsy listing data. Please try again.");
            }
            return toCompactListing(listingId, canonicalUrl, objectMapper.readTree(response.body()));
        } catch (ResponseStatusException | EtsyTransientException ex) {
            throw ex;
        } catch (Exception ex) {
            meterRegistry.counter("listifyai.etsy.error", "type", "timeout_or_unknown").increment();
            throw new EtsyTransientException("Could not fetch Etsy listing data. Please try again.", ex);
        }
    }

    public CompactEtsyListing etsyFallback(String listingId, String canonicalUrl, Throwable throwable) {
        if (throwable instanceof ResponseStatusException rse) {
            throw rse;
        }
        if (throwable instanceof EtsyTransientException ete) {
            throw ete;
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Etsy API is temporarily unavailable.", throwable);
    }

    private CompactEtsyListing toCompactListing(String listingId, String canonicalUrl, JsonNode root) {
        List<String> tags = new ArrayList<>();
        root.path("tags").forEach(tag -> {
            String value = clean(tag.asText());
            if (!value.isBlank()) {
                tags.add(value);
            }
        });

        Map<String, Object> signals = new LinkedHashMap<>();
        signals.put("state", clean(root.path("state").asText()));
        signals.put("quantity", root.path("quantity").asInt(0));
        signals.put("hasVariations", root.path("has_variations").asBoolean(false));
        signals.put("views", root.path("views").asInt(0));
        signals.put("numFavorers", root.path("num_favorers").asInt(0));

        return new CompactEtsyListing(
                listingId,
                canonicalUrl,
                clean(root.path("title").asText()),
                trimDescription(clean(root.path("description").asText())),
                tags,
                clean(root.path("taxonomy_path").toString()),
                clean(root.path("who_made").asText()),
                clean(root.path("when_made").asText()),
                clean(root.path("is_supply").asText()),
                clean(root.path("price").path("amount").asText()),
                clean(root.path("price").path("currency_code").asText()),
                signals);
    }

    private String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String trimDescription(String description) {
        if (description.length() <= 1800) {
            return description;
        }
        return description.substring(0, 1800).trim();
    }
}
