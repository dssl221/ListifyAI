package com.listifyai.dto;

import java.util.UUID;

public record UserProfileDTO(
        UUID id,
        String email,
        String displayName,
        boolean namePromptCompleted,
        String plan,
        String role,
        int listingsUsedThisMonth,
        int monthlyLimit,
        boolean unlimited,
        String creditsRemaining
) {}
