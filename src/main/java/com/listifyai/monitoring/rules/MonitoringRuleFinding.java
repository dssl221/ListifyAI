package com.listifyai.monitoring.rules;

import com.listifyai.monitoring.MonitoringEnums.MonitoringSeverity;

import java.util.Map;

public record MonitoringRuleFinding(
        String type,
        MonitoringSeverity severity,
        int opportunityScore,
        String title,
        String message,
        Map<String, Object> details,
        String ruleVersion,
        String rulesEngineVersion
) {
    public MonitoringRuleFinding(String type,
                                 MonitoringSeverity severity,
                                 int opportunityScore,
                                 String title,
                                 String message,
                                 Map<String, Object> details) {
        this(type, severity, opportunityScore, title, message, details, "1.0", "1.0");
    }

    public MonitoringRuleFinding withVersions(String ruleVersion, String rulesEngineVersion) {
        return new MonitoringRuleFinding(type, severity, opportunityScore, title, message, details, ruleVersion, rulesEngineVersion);
    }
}
