package com.listifyai.monitoring.rules;

import com.listifyai.monitoring.MonitoringProperties;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@SuppressWarnings({"null", "unchecked", "unused"})
public class MonitoringRuleEngine {

    private final List<MonitoringRule> rules;
    private final MonitoringProperties properties;

    public MonitoringRuleEngine(List<MonitoringRule> rules, MonitoringProperties properties) {
        this.rules = rules;
        this.properties = properties;
    }

    public List<MonitoringRuleFinding> evaluate(MonitoringRuleContext context) {
        return rules.stream()
                .map(rule -> rule.evaluate(context))
                .flatMap(OptionalCompat::stream)
                .map(finding -> finding.withVersions(properties.getRules().getVersion(), properties.getRules().getVersion()))
                .sorted(Comparator.comparingInt(MonitoringRuleFinding::opportunityScore).reversed())
                .toList();
    }

    private static final class OptionalCompat {
        private OptionalCompat() {}
        static <T> java.util.stream.Stream<T> stream(java.util.Optional<T> optional) {
            return optional.map(java.util.stream.Stream::of).orElseGet(java.util.stream.Stream::empty);
        }
    }
}
