package com.listifyai.monitoring.rules;

import com.listifyai.monitoring.MonitoringEnums.MonitoringSeverity;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class LowTagDiversityRule implements MonitoringRule {

    @Override
    public Optional<MonitoringRuleFinding> evaluate(MonitoringRuleContext context) {
        Set<String> normalized = new HashSet<>();
        for (String tag : context.tags()) {
            if (tag != null && !tag.isBlank()) {
                normalized.add(tag.toLowerCase(Locale.ROOT).trim());
            }
        }
        if (context.tags().size() >= 10 && normalized.size() >= 8) {
            return Optional.empty();
        }
        return Optional.of(new MonitoringRuleFinding(
                "LOW_TAG_DIVERSITY",
                MonitoringSeverity.MEDIUM,
                30,
                "Tag coverage is thin",
                "This listing has too few distinct tags to cover multiple buyer searches.",
                Map.of("tagCount", context.tags().size(), "distinctTagCount", normalized.size())
        ));
    }
}
