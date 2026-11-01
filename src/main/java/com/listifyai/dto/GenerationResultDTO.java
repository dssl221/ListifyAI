package com.listifyai.dto;

import java.util.UUID;

public record GenerationResultDTO(
        UUID listingId,
        EtsyOutputDTO etsy,
        ShopifyOutputDTO shopify,
        String creditsRemaining,
        boolean unlimited,
        boolean shippingWarning,
        String shippingWarningMessage
) {}
