package com.listifyai.monitoring.rules;

import com.listifyai.monitoring.MonitoringEnums.MonitoringSeverity;
import com.listifyai.monitoring.MonitoringProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class ScoreDropRule implements MonitoringRule {

    private final MonitoringProperties properties;

    public ScoreDropRule(MonitoringProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<MonitoringRuleFinding> evaluate(MonitoringRuleContext context) {
        if (context.previous() == null || context.previous().getScore() == null) {
            return Optional.empty();
        }
        int previous = context.previous().getScore();
        int current = context.current().getScore();
        int drop = previous - current;
        if (drop < properties.getScoreDropThreshold()) {
            return Optional.empty();
        }
        return Optional.of(new MonitoringRuleFinding(
                "SCORE_DROP",
                drop >= 20 ? MonitoringSeverity.HIGH : MonitoringSeverity.MEDIUM,
                Math.min(90, 40 + drop),
                "Listing score dropped",
                "The lightweight SEO score dropped since the last snapshot. Review recent title, tag, or description changes.",
                Map.of("previousScore", previous, "currentScore", current, "drop", drop)
        ));
    }
}
