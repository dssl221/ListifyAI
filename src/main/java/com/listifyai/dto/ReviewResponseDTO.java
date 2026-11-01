package com.listifyai.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReviewResponseDTO(
        UUID id,
        String displayName,
        String sellerRole,
        Integer rating,
        String body,
        OffsetDateTime createdAt
) {}
