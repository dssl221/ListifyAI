package com.listifyai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequestDTO(
        @NotBlank @Size(max = 120) String displayName,
        @NotBlank @Size(max = 120) String sellerRole,
        @NotNull @Min(1) @Max(5) Integer rating,
        @NotBlank @Size(min = 12, max = 1000) String body
) {}
