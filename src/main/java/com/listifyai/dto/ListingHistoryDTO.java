package com.listifyai.dto;

import java.time.Instant;
import java.util.UUID;

public record ListingHistoryDTO(
        UUID id,
        String productName,
        String platform,
        String tone,
        boolean isSaved,
        Instant createdAt
) {}
