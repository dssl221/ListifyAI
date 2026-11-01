package com.listifyai.service;

import com.listifyai.dto.AdminDtos.*;
import com.listifyai.repository.*;
import com.listifyai.monitoring.MonitoringEnums.MonitoringJobStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final double INPUT_COST_PER_TOKEN = 3.0 / 1_000_000;
    private static final double OUTPUT_COST_PER_TOKEN = 15.0 / 1_000_000;

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final BulkJobRepository bulkJobRepository;
    private final MonitoredListingRepository monitoredListingRepository;
    private final MonitoringJobRepository monitoringJobRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final ClaudeUsageLogRepository claudeUsageLogRepository;

    public AdminDashboardService(UserRepository userRepository,
                                 ListingRepository listingRepository,
                                 BulkJobRepository bulkJobRepository,
                                 MonitoredListingRepository monitoredListingRepository,
                                 MonitoringJobRepository monitoringJobRepository,
                                 SupportTicketRepository supportTicketRepository,
                                 ClaudeUsageLogRepository claudeUsageLogRepository) {
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.bulkJobRepository = bulkJobRepository;
        this.monitoredListingRepository = monitoredListingRepository;
        this.monitoringJobRepository = monitoringJobRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.claudeUsageLogRepository = claudeUsageLogRepository;
    }

    public AdminDashboardDTO getDashboard() {
        OffsetDateTime monthStart = OffsetDateTime.now().minusDays(30);
        OffsetDateTime todayStart = OffsetDateTime.now().toLocalDate().atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        long freeUsers = userRepository.countByPlan("free");
        long starterUsers = userRepository.countByPlan("starter");
        long growthUsers = userRepository.countByPlan("growth");
        long proUsers = userRepository.countByPlan("pro");
        long inputTokens = nvl(claudeUsageLogRepository.sumInputTokensSince(monthStart));
        long outputTokens = nvl(claudeUsageLogRepository.sumOutputTokensSince(monthStart));
        double mrr = (starterUsers * 19.0) + (growthUsers * 29.0) + (proUsers * 50.0);

        return new AdminDashboardDTO(
                userRepository.count(),
                userRepository.countByCreatedAtAfter(monthStart),
                starterUsers + growthUsers + proUsers,
                freeUsers,
                starterUsers,
                growthUsers,
                proUsers,
                listingRepository.count(),
                bulkJobRepository.count(),
                bulkJobRepository.countByStatus("failed"),
                monitoredListingRepository.count(),
                monitoringJobRepository.countByStatus(MonitoringJobStatus.RUNNING),
                monitoringJobRepository.countByStatus(MonitoringJobStatus.FAILED),
                supportTicketRepository.countByStatusIn(List.of("open", "in_progress")),
                nvl(claudeUsageLogRepository.countSince(todayStart)),
                nvl(claudeUsageLogRepository.countSince(monthStart)),
                inputTokens + outputTokens,
                (inputTokens * INPUT_COST_PER_TOKEN) + (outputTokens * OUTPUT_COST_PER_TOKEN),
                mrr,
                toDailyStats(userRepository.countSignupsByDaySince(monthStart)),
                toDailyStats(listingRepository.countListingsByDaySince(monthStart))
        );
    }

    public AdminClaudeUsageDTO getClaudeUsage() {
        OffsetDateTime monthStart = OffsetDateTime.now().minusDays(30);
        OffsetDateTime todayStart = OffsetDateTime.now().toLocalDate().atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        long todayInput = nvl(claudeUsageLogRepository.sumInputTokensSince(todayStart));
        long todayOutput = nvl(claudeUsageLogRepository.sumOutputTokensSince(todayStart));
        long monthInput = nvl(claudeUsageLogRepository.sumInputTokensSince(monthStart));
        long monthOutput = nvl(claudeUsageLogRepository.sumOutputTokensSince(monthStart));
        Map<String, Long> callsByType = new LinkedHashMap<>();
        for (Object[] row : claudeUsageLogRepository.usageByCallTypeSince(monthStart)) {
            callsByType.put(String.valueOf(row[0]), toLong(row[1]));
        }
        List<TopUserTokenDTO> topUsers = claudeUsageLogRepository.topUsersByTokensSince(monthStart).stream()
                .limit(10)
                .map(row -> new TopUserTokenDTO((java.util.UUID) row[0], String.valueOf(row[1]), toLong(row[2])))
                .toList();
        return new AdminClaudeUsageDTO(
                nvl(claudeUsageLogRepository.countSince(todayStart)),
                nvl(claudeUsageLogRepository.countSince(monthStart)),
                todayInput + todayOutput,
                monthInput + monthOutput,
                (todayInput * INPUT_COST_PER_TOKEN) + (todayOutput * OUTPUT_COST_PER_TOKEN),
                (monthInput * INPUT_COST_PER_TOKEN) + (monthOutput * OUTPUT_COST_PER_TOKEN),
                callsByType,
                topUsers
        );
    }

    private List<DailyStatDTO> toDailyStats(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new DailyStatDTO(String.valueOf(row[0]), toLong(row[1])))
                .toList();
    }

    private long nvl(Long value) {
        return value == null ? 0 : value;
    }

    private long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
    }
}
