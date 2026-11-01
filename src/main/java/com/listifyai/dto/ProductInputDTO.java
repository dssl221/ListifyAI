package com.listifyai.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record ProductInputDTO(

        @NotBlank(message = "Product name is required")
        String productName,

        String material,

        String size,

        String targetBuyer,

        String useCase,

        String occasion,

        @NotBlank(message = "Platform is required")
        String platform,

        @NotBlank(message = "Tone is required")
        String tone,

        @DecimalMin(value = "0.0", inclusive = true)
        @DecimalMax(value = "999.99")
        BigDecimal shippingPrice
) {}
