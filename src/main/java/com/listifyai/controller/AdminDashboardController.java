package com.listifyai.controller;

import com.listifyai.dto.AdminDashboardResponse;
import com.listifyai.monitoring.MonitoringEnums.MonitoringJobStatus;
import com.listifyai.repository.AuditLogRepository;
import com.listifyai.repository.MonitoredListingRepository;
import com.listifyai.repository.MonitoringEventRepository;
import com.listifyai.repository.MonitoringJobRepository;
import com.listifyai.repository.UserRepository;
import com.listifyai.security.AdminOnly;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@AdminOnly
public class AdminDashboardController {

    private static final Map<String, Long> PLAN_PRICES = Map.of(
            "starter", 1900L,
            "growth", 4900L,
            "pro", 9900L
    );

    private final UserRepository userRepository;
    private final MonitoredListingRepository monitoredListingRepository;
    private final MonitoringJobRepository jobRepository;
    private final MonitoringEventRepository eventRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminDashboardController(UserRepository userRepository,
                                    MonitoredListingRepository monitoredListingRepository,
                                    MonitoringJobRepository jobRepository,
                                    MonitoringEventRepository eventRepository,
                                    AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.monitoredListingRepository = monitoredListingRepository;
        this.jobRepository = jobRepository;
        this.eventRepository = eventRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        Map<String, Long> usersByPlan = usersByPlan();
        long mrr = usersByPlan.entrySet().stream()
                .mapToLong(entry -> PLAN_PRICES.getOrDefault(entry.getKey(), 0L) * entry.getValue())
                .sum();
        List<AdminDashboardResponse.TopFailingRule> topFailingRules = eventRepository.topFailingRulesLast7Days()
                .stream()
                .map(row -> new AdminDashboardResponse.TopFailingRule((String) row[0], toLong(row[1])))
                .toList();
        return new AdminDashboardResponse(
                userRepository.count(),
                usersByPlan,
                mrr,
                monitoredListingRepository.countByMonitoringEnabledTrue(),
                jobRepository.countByStatus(MonitoringJobStatus.PENDING),
                auditLogRepository.countClaudeCallsToday(),
                topFailingRules
        );
    }

    private Map<String, Long> usersByPlan() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : userRepository.countUsersByPlan()) {
            result.put((String) row[0], toLong(row[1]));
        }
        return result;
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
