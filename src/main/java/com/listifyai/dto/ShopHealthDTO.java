package com.listifyai.dto;

import java.time.Instant;
import java.util.List;

public record ShopHealthDTO(
        int overallScore,
        String grade,
        String headline,
        int listingsAnalysed,
        List<HealthSignalDTO> signals,
        List<String> topFixes,
        Instant computedAt
) {
    public record HealthSignalDTO(
            String area,
            String status,
            int score,
            String finding,
            String suggestion
    ) {}
}
