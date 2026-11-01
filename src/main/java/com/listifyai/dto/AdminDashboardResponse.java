package com.listifyai.dto;

import java.util.List;
import java.util.Map;

public record AdminDashboardResponse(
        long totalUsers,
        Map<String, Long> usersByPlan,
        long mrr,
        long activeMonitoredListings,
        long queueDepth,
        long claudeCallsToday,
        List<TopFailingRule> topFailingRules
) {
    public record TopFailingRule(String ruleName, long eventCount) {}
}
