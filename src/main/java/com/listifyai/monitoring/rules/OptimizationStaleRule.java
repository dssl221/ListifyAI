package com.listifyai.monitoring.rules;

import com.listifyai.monitoring.MonitoringEnums.MonitoringSeverity;
import com.listifyai.monitoring.MonitoringEnums.OptimizationState;
import com.listifyai.monitoring.MonitoringProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Component
public class OptimizationStaleRule implements MonitoringRule {

    private final MonitoringProperties properties;

    public OptimizationStaleRule(MonitoringProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<MonitoringRuleFinding> evaluate(MonitoringRuleContext context) {
        if (context.listing().getOptimizationState() != OptimizationState.OPTIMIZED
                || context.listing().getLastSnapshotAt() == null) {
            return Optional.empty();
        }
        long days = Duration.between(context.listing().getLastSnapshotAt(), context.now()).toDays();
        if (days < properties.getOptimizationStaleDays()) {
            return Optional.empty();
        }
        return Optional.of(new MonitoringRuleFinding(
                "OPTIMIZATION_STALE",
                MonitoringSeverity.MEDIUM,
                25,
                "Previous optimization may be stale",
                "This listing was optimized before, but enough time has passed to justify a lightweight review.",
                Map.of("daysSinceOptimization", days)
        ));
    }
}
