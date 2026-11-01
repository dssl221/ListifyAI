package com.listifyai.dto;

import java.util.List;

public record ShopifyOutputDTO(
        String seoTitle,
        String metaDescription,
        String description,
        List<String> bullets,
        int qualityScore,
        List<String> tips
) {}
