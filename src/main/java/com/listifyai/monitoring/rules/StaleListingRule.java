package com.listifyai.monitoring.rules;

import com.listifyai.monitoring.MonitoringEnums.MonitoringSeverity;
import com.listifyai.monitoring.MonitoringProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Component
public class StaleListingRule implements MonitoringRule {

    private final MonitoringProperties properties;

    public StaleListingRule(MonitoringProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<MonitoringRuleFinding> evaluate(MonitoringRuleContext context) {
        if (context.listing().getCreatedAt() == null) {
            return Optional.empty();
        }
        long ageDays = Duration.between(context.listing().getCreatedAt(), context.now()).toDays();
        if (ageDays < properties.getStaleListingDays()) {
            return Optional.empty();
        }
        return Optional.of(new MonitoringRuleFinding(
                "STALE_LISTING",
                MonitoringSeverity.LOW,
                20,
                "Listing has not been refreshed recently",
                "Older listings can drift away from buyer language. Review tags and title freshness before running a full AI rewrite.",
                Map.of("ageDays", ageDays)
        ));
    }
}
