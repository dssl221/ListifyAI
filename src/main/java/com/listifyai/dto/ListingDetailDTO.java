package com.listifyai.dto;

import java.time.Instant;
import java.util.UUID;

public record ListingDetailDTO(
        UUID id,
        String productName,
        String platform,
        String tone,
        EtsyOutputDTO etsyOutput,
        ShopifyOutputDTO shopifyOutput,
        boolean isSaved,
        Instant createdAt
) {}
