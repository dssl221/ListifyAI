package com.listifyai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.listifyai.dto.EtsyOutputDTO;
import com.listifyai.dto.ProductInputDTO;
import com.listifyai.dto.ShopifyOutputDTO;
import com.listifyai.dto.TagDTO;
import com.listifyai.entity.BulkJobItem;
import com.listifyai.entity.Listing;
import com.listifyai.entity.User;
import com.listifyai.exception.ClaudeApiException;
import com.listifyai.exception.ListingLimitException;
import com.listifyai.repository.BulkJobItemRepository;
import com.listifyai.repository.ListingRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Component
@SuppressWarnings({"null", "unchecked", "unused"})
public class BulkJobProcessor {

    private static final Logger log = LoggerFactory.getLogger(BulkJobProcessor.class);

    private final BulkJobItemRepository bulkJobItemRepository;
    private final ListingRepository listingRepository;
    private final ClaudeService claudeService;
    private final PlanClaudeRateLimiter planClaudeRateLimiter;
    private final UsageService usageService;
    private final ObjectMapper objectMapper;

    public BulkJobProcessor(BulkJobItemRepository bulkJobItemRepository,
                            ListingRepository listingRepository,
                            ClaudeService claudeService,
                            PlanClaudeRateLimiter planClaudeRateLimiter,
                            UsageService usageService,
                            ObjectMapper objectMapper) {
        this.bulkJobItemRepository = bulkJobItemRepository;
        this.listingRepository = listingRepository;
        this.claudeService = claudeService;
        this.planClaudeRateLimiter = planClaudeRateLimiter;
        this.usageService = usageService;
        this.objectMapper = objectMapper;
    }

    public void processSingleItem(BulkJobItem item) {
        try {
            User user = item.getJob().getUser();
            ProductInputDTO input = objectMapper.readValue(item.getInputPayload(), ProductInputDTO.class);

            usageService.checkAndIncrement(user);
            planClaudeRateLimiter.checkLimit(user);

            EtsyOutputDTO etsy = null;
            ShopifyOutputDTO shopify = null;

            switch (input.platform().toLowerCase()) {
                case "etsy" -> etsy = claudeService.generateEtsy(input).join();
                case "shopify" -> shopify = claudeService.generateShopify(input).join();
                case "both" -> {
                    ClaudeService.GenerationResult result = claudeService.generateBoth(input);
                    etsy = result.etsy();
                    shopify = result.shopify();
                }
                default -> throw new IllegalArgumentException("Unknown platform: " + input.platform());
            }

            Listing listing = new Listing();
            listing.setUser(user);
            listing.setProductName(input.productName());
            listing.setRawInput(toJson(input));
            listing.setPlatform(input.platform());
            listing.setTone(input.tone());
            listing.setEtsyOutput(toJson(etsy));
            listing.setShopifyOutput(toJson(shopify));
            listing.setIsSaved(false);
            listingRepository.save(listing);

            item.setStatus("COMPLETED");
            item.setResultReference(listing.getId().toString());
            item.setLockedBy(null);
            bulkJobItemRepository.save(item);
            log.info("Item {} completed successfully", item.getId());

        } catch (ListingLimitException ex) {
            item.setStatus("DEAD_LETTER");
            item.setLastError("Monthly limit reached");
            item.setLockedBy(null);
            bulkJobItemRepository.save(item);
            log.error("Item {} failed with DEAD_LETTER: Monthly limit reached", item.getId());
        } catch (ClaudeApiException ex) {
            if (item.getAttemptCount() < 3) {
                item.setStatus("RETRYING");
                log.warn("Item {} failed, scheduling retry (attempt {}/3): {}", item.getId(), item.getAttemptCount(), ex.getMessage());
            } else {
                item.setStatus("DEAD_LETTER");
                log.error("Item {} failed with DEAD_LETTER (max retries reached): {}", item.getId(), ex.getMessage());
            }
            item.setLastError(ex.getMessage());
            item.setLockedBy(null);
            bulkJobItemRepository.save(item);
        } catch (Exception ex) {
            log.error("Item {} failed unexpectedly", item.getId(), ex);
            item.setStatus("DEAD_LETTER");
            item.setLastError(ex.getClass().getName() + ": " + ex.getMessage());
            item.setLockedBy(null);
            bulkJobItemRepository.save(item);
            log.error("Item {} failed with DEAD_LETTER (unexpected error)", item.getId());
        }
    }

    public String buildResultCsvDataUrl(List<BulkJobItem> items) throws IOException {
        StringWriter sw = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder()
                .setHeader("product_name", "platform", "etsy_title", "etsy_description",
                        "etsy_tags_with_competition", "shopify_seo_title", "shopify_meta_description",
                        "shopify_description", "shopify_bullets", "error")
                .build())) {
            for (BulkJobItem item : items) {
                try {
                    ProductInputDTO input = objectMapper.readValue(item.getInputPayload(), ProductInputDTO.class);
                    String error = item.getLastError() != null ? item.getLastError() : "";
                    
                    if ("COMPLETED".equals(item.getStatus()) && item.getResultReference() != null) {
                        Listing listing = listingRepository.findById(java.util.UUID.fromString(item.getResultReference())).orElse(null);
                        if (listing != null) {
                            EtsyOutputDTO etsy = listing.getEtsyOutput() != null ? objectMapper.readValue(listing.getEtsyOutput(), EtsyOutputDTO.class) : null;
                            ShopifyOutputDTO shopify = listing.getShopifyOutput() != null ? objectMapper.readValue(listing.getShopifyOutput(), ShopifyOutputDTO.class) : null;
                            
                            printer.printRecord(
                                    csvSafe(input.productName()),
                                    csvSafe(input.platform()),
                                    csvSafe(etsy != null ? String.join(" | ", etsy.titles()) : ""),
                                    csvSafe(etsy != null ? etsy.description() : ""),
                                    csvSafe(etsy != null ? formatTags(etsy.tags()) : ""),
                                    csvSafe(shopify != null ? shopify.seoTitle() : ""),
                                    csvSafe(shopify != null ? shopify.metaDescription() : ""),
                                    csvSafe(shopify != null ? shopify.description() : ""),
                                    csvSafe(shopify != null ? String.join(" | ", shopify.bullets()) : ""),
                                    csvSafe(error)
                            );
                            continue;
                        }
                    }
                    
                    printer.printRecord(
                            csvSafe(input.productName()),
                            csvSafe(input.platform()),
                            "", "", "", "", "", "", "",
                            csvSafe(error)
                    );
                } catch (Exception e) {
                    log.error("Error generating CSV row for item {}", item.getId(), e);
                }
            }
        }
        byte[] csvBytes = sw.toString().getBytes(StandardCharsets.UTF_8);
        String base64 = Base64.getEncoder().encodeToString(csvBytes);
        return "data:text/csv;base64," + base64;
    }

    private String formatTags(List<TagDTO> tags) {
        if (tags == null) {
            return "";
        }
        return tags.stream()
                .filter(t -> t != null && t.tag() != null && !t.tag().isBlank())
                .map(t -> t.tag() + " [" + (t.competition() != null ? t.competition() : "?") + "]")
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException ex) {
            throw new ClaudeApiException("JSON serialization error", ex);
        }
    }

    private String csvSafe(String value) {
        if (value == null || value.isEmpty()) {
            return value == null ? "" : value;
        }
        char first = value.charAt(0);
        return first == '=' || first == '+' || first == '-' || first == '@'
                ? "'" + value
                : value;
    }
}
