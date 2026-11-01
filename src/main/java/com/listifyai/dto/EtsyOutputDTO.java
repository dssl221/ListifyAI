package com.listifyai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record EtsyOutputDTO(
        @JsonProperty("titles")
        List<String> titles,
        String description,
        List<TagDTO> tags,
        int qualityScore,
        List<String> tips
) {}
