package com.listifyai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.listifyai.dto.EtsyOutputDTO;
import com.listifyai.dto.GenerationResultDTO;
import com.listifyai.dto.ListingDetailDTO;
import com.listifyai.dto.ListingHistoryDTO;
import com.listifyai.dto.ProductInputDTO;
import com.listifyai.dto.ShopifyOutputDTO;
import com.listifyai.entity.Listing;
import com.listifyai.entity.User;
import com.listifyai.exception.ClaudeApiException;
import com.listifyai.exception.ListingLimitException;
import com.listifyai.repository.ListingRepository;
import com.listifyai.security.AuthUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@SuppressWarnings({"null", "unchecked", "unused"})
public class ListingService {

    private static final Set<String> BOTH_PLATFORMS_ALLOWED = Set.of("growth", "pro");

    private final ListingRepository listingRepository;
    private final ClaudeService claudeService;
    private final PlanClaudeRateLimiter planClaudeRateLimiter;
    private final UsageService usageService;
    private final ObjectMapper objectMapper;

    public ListingService(ListingRepository listingRepository,
                          ClaudeService claudeService,
                          PlanClaudeRateLimiter planClaudeRateLimiter,
                          UsageService usageService,
                          ObjectMapper objectMapper) {
        this.listingRepository = listingRepository;
        this.claudeService = claudeService;
        this.planClaudeRateLimiter = planClaudeRateLimiter;
        this.usageService = usageService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public GenerationResultDTO generate(ProductInputDTO input, User user) {
        AuthUtils.assertNotSuspended(user);

        String plan = user.getPlan() != null ? user.getPlan().toLowerCase() : "";
        if ("both".equalsIgnoreCase(input.platform())
                && !BOTH_PLATFORMS_ALLOWED.contains(plan)) {
            throw new ListingLimitException(
                    "Generating for both platforms requires Growth plan or higher. Upgrade to continue.");
        }

        usageService.checkAndIncrement(user);
        planClaudeRateLimiter.checkLimit(user);

        EtsyOutputDTO etsyOutput = null;
        ShopifyOutputDTO shopifyOutput = null;

        switch (input.platform().toLowerCase()) {
            case "etsy" -> etsyOutput = claudeService.generateEtsy(input).join();
            case "shopify" -> shopifyOutput = claudeService.generateShopify(input).join();
            case "both" -> {
                ClaudeService.GenerationResult result = claudeService.generateBoth(input);
                etsyOutput = result.etsy();
                shopifyOutput = result.shopify();
            }
            default -> throw new IllegalArgumentException(
                    "Unknown platform: " + input.platform() + ". Must be etsy, shopify, or both.");
        }

        boolean shippingWarning = false;
        String shippingWarningMessage = null;

        if (input.shippingPrice() != null
                && input.platform() != null
                && (input.platform().equals("etsy") || input.platform().equals("both"))
                && input.shippingPrice().compareTo(new BigDecimal("6.00")) > 0) {
            shippingWarning = true;
            shippingWarningMessage = "Shipping over $6 may reduce your Etsy search "
                    + "ranking. Consider offering free shipping or reducing to $5.99 max.";
        }

        Listing listing = new Listing();
        listing.setUser(user);
        listing.setProductName(input.productName());
        listing.setRawInput(toJson(input));
        listing.setPlatform(input.platform());
        listing.setTone(input.tone());
        listing.setEtsyOutput(toJson(etsyOutput));
        listing.setShopifyOutput(toJson(shopifyOutput));
        listing.setIsSaved(false);

        listing = listingRepository.save(listing);

        return new GenerationResultDTO(
                listing.getId(),
                etsyOutput,
                shopifyOutput,
                usageService.getRemainingCredits(listing.getUser()),
                usageService.isUnlimitedPlan(listing.getUser().getPlan()),
                shippingWarning,
                shippingWarningMessage
        );
    }

    public Page<ListingHistoryDTO> getHistory(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return listingRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::toHistoryDTO);
    }

    public Page<ListingHistoryDTO> getHistory(
            User user,
            int page,
            int size,
            int effectiveSize,
            int maxHistorySize) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ListingHistoryDTO> historyPage = listingRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::toHistoryDTO);

        List<ListingHistoryDTO> content = historyPage.getContent();
        if (effectiveSize < content.size()) {
            content = content.subList(0, effectiveSize);
        }

        long cappedTotal = maxHistorySize == Integer.MAX_VALUE
                ? historyPage.getTotalElements()
                : Math.min(historyPage.getTotalElements(), (long) maxHistorySize);

        return new PageImpl<>(content, pageable, cappedTotal);
    }

    public ListingDetailDTO getDetail(UUID id, User user) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Listing not found: " + id));

        if (!listing.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You do not have permission to view this listing.");
        }

        return new ListingDetailDTO(
                listing.getId(),
                listing.getProductName(),
                listing.getPlatform(),
                listing.getTone(),
                fromJson(listing.getEtsyOutput(), EtsyOutputDTO.class),
                fromJson(listing.getShopifyOutput(), ShopifyOutputDTO.class),
                Boolean.TRUE.equals(listing.getIsSaved()),
                listing.getCreatedAt() != null ? listing.getCreatedAt().toInstant() : null
        );
    }

    @Transactional
    public void toggleSave(UUID listingId, User user) {
        @SuppressWarnings("null")
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Listing not found: " + listingId));

        if (!listing.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You do not have permission to modify this listing.");
        }

        listing.setIsSaved(!listing.getIsSaved());
        listingRepository.save(listing);
    }

    @Transactional
    public void deleteListing(UUID id, User user) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Listing not found"));

        if (!listing.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Cannot delete another user's listing");
        }

        listingRepository.delete(listing);
    }

    private ListingHistoryDTO toHistoryDTO(Listing listing) {
        return new ListingHistoryDTO(
                listing.getId(),
                listing.getProductName(),
                listing.getPlatform(),
                listing.getTone(),
                Boolean.TRUE.equals(listing.getIsSaved()),
                listing.getCreatedAt() != null ? listing.getCreatedAt().toInstant() : null
        );
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new ClaudeApiException("JSON serialization error: " + e.getMessage(), e);
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new ClaudeApiException("JSON deserialization error: " + e.getMessage(), e);
        }
    }
}
