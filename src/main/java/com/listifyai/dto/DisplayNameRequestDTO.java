package com.listifyai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DisplayNameRequestDTO(
        @NotBlank(message = "Display name is required")
        @Size(max = 50, message = "Display name must be 50 characters or fewer")
        String displayName
) {}
