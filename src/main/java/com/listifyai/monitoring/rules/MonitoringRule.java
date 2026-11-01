package com.listifyai.monitoring.rules;

import java.util.Optional;

public interface MonitoringRule {
    Optional<MonitoringRuleFinding> evaluate(MonitoringRuleContext context);
}
