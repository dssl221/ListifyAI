package com.listifyai.monitoring;

import com.listifyai.dto.EtsyAuditDtos.CompactEtsyListing;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ListingScoreService {

    public int score(CompactEtsyListing listing) {
        int score = 100;
        String title = safe(listing.title());
        String description = safe(listing.description());
        List<String> tags = listing.tags() == null ? List.of() : listing.tags();

        if (title.length() < 80) {
            score -= 15;
        } else if (title.length() < 110) {
            score -= 7;
        }
        if (title.length() > 140) {
            score -= 10;
        }
        if (description.length() < 300) {
            score -= 15;
        }
        if (tags.size() < 13) {
            score -= (13 - tags.size()) * 3;
        }
        Set<String> distinct = new HashSet<>();
        for (String tag : tags) {
            if (tag != null && !tag.isBlank()) {
                distinct.add(tag.toLowerCase(Locale.ROOT).trim());
            }
        }
        if (distinct.size() < tags.size()) {
            score -= Math.min(12, (tags.size() - distinct.size()) * 4);
        }
        if (tags.stream().noneMatch(tag -> tag != null && tag.trim().contains(" "))) {
            score -= 10;
        }
        if (!(title + " " + description + " " + String.join(" ", tags)).toLowerCase(Locale.ROOT).contains("gift")) {
            score -= 5;
        }
        return Math.max(0, Math.min(100, score));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
