package com.listifyai.monitoring.rules;

import com.listifyai.monitoring.MonitoringEnums.MonitoringSeverity;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
public class MissingGiftingIntentRule implements MonitoringRule {

    @Override
    public Optional<MonitoringRuleFinding> evaluate(MonitoringRuleContext context) {
        String haystack = (context.current().getTitle() + " " + context.current().getDescription() + " " + String.join(" ", context.tags()))
                .toLowerCase(Locale.ROOT);
        String tagHaystack = String.join(" ", context.tags()).toLowerCase(Locale.ROOT);
        boolean giftable = tagHaystack.contains("personalized")
                || tagHaystack.contains("custom")
                || tagHaystack.contains("mom")
                || tagHaystack.contains("wedding")
                || tagHaystack.contains("birthday")
                || tagHaystack.contains("necklace")
                || tagHaystack.contains("candle");
        boolean hasGifting = haystack.contains("gift") || haystack.contains("present") || haystack.contains("stocking stuffer");
        if (!giftable || hasGifting) {
            return Optional.empty();
        }
        return Optional.of(new MonitoringRuleFinding(
                "MISSING_GIFTING_INTENT",
                MonitoringSeverity.MEDIUM,
                25,
                "Gift intent is missing",
                "This looks giftable, but the title and tags do not clearly target shoppers buying for someone else.",
                Map.of("reason", "giftable_terms_without_gifting_language")
        ));
    }
}
