package com.listifyai.monitoring.rules;

import com.listifyai.monitoring.MonitoringEnums.MonitoringSeverity;
import com.listifyai.monitoring.MonitoringProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Component
public class UnchangedListingRule implements MonitoringRule {

    private final MonitoringProperties properties;

    public UnchangedListingRule(MonitoringProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<MonitoringRuleFinding> evaluate(MonitoringRuleContext context) {
        if (context.previous() == null || context.current().getCapturedAt() == null || context.previous().getCapturedAt() == null) {
            return Optional.empty();
        }
        boolean unchanged = context.current().getChecksum().equals(context.previous().getChecksum());
        long days = Duration.between(context.previous().getCapturedAt(), context.current().getCapturedAt()).toDays();
        if (!unchanged || days < properties.getUnchangedListingDays()) {
            return Optional.empty();
        }
        return Optional.of(new MonitoringRuleFinding(
                "UNCHANGED_LISTING",
                MonitoringSeverity.LOW,
                15,
                "Listing content is unchanged",
                "This listing has stayed unchanged across monitoring snapshots. Consider a manual review if traffic has softened.",
                Map.of("unchangedDays", days)
        ));
    }
}
