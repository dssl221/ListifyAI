package com.listifyai.monitoring.rules;

import com.listifyai.monitoring.MonitoringEnums.MonitoringSeverity;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class BroadTagRule implements MonitoringRule {

    private static final Set<String> BROAD_TAGS = Set.of(
            "gift", "gifts", "handmade", "decor", "jewelry", "art", "print", "shirt",
            "home decor", "wall art", "necklace", "bracelet", "ring", "candle",
            "vintage", "cute", "funny", "unique", "custom", "personalized", "baby",
            "women", "men", "kids", "wood", "ceramic", "leather", "floral", "boho",
            "minimalist", "aesthetic", "cottagecore"
    );

    @Override
    public Optional<MonitoringRuleFinding> evaluate(MonitoringRuleContext context) {
        List<String> normalizedTags = context.tags().stream()
                .map(tag -> tag.toLowerCase(Locale.ROOT).trim())
                .filter(tag -> !tag.isBlank())
                .toList();
        List<String> broad = normalizedTags.stream()
                .filter(BROAD_TAGS::contains)
                .distinct()
                .toList();
        long longTailTagCount = normalizedTags.stream()
                .filter(this::hasAtLeastTwoWords)
                .count();
        int shortTagCount = (int) Math.max(0, normalizedTags.size() - longTailTagCount);

        if (broad.size() < 3 && longTailTagCount >= 4) {
            return Optional.empty();
        }

        Map<String, Object> details = new LinkedHashMap<>();
        if (broad.size() >= 3) {
            details.put("broadTags", broad);
        }
        if (longTailTagCount < 4) {
            details.put("shortTagCount", shortTagCount);
        }

        return Optional.of(new MonitoringRuleFinding(
                "BROAD_TAG_OVERUSE",
                MonitoringSeverity.MEDIUM,
                20 + Math.min(20, Math.max(broad.size(), shortTagCount) * 5),
                "Broad tags are doing too much work",
                "This listing leans on generic Etsy tags. Replace some with buyer, material, style, or occasion phrases.",
                details
        ));
    }

    private boolean hasAtLeastTwoWords(String tag) {
        return tag.split("\\s+").length >= 2;
    }
}
