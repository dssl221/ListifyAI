package com.listifyai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AdminDtos {
    private AdminDtos() {}

    public record AdminDashboardDTO(long totalUsers, long activeUsersThisMonth, long paidUsers,
                                    long freeUsers, long starterUsers, long growthUsers, long proUsers,
                                    long totalListingsGenerated, long totalBulkJobs, long failedBulkJobs,
                                    long totalMonitoredListings, long activeMonitoringJobs, long failedMonitoringJobs,
                                    long openSupportTickets, long totalClaudeCallsToday,
                                    long totalClaudeCallsThisMonth, long totalTokensThisMonth,
                                    double estimatedCostThisMonth, double mrr,
                                    List<DailyStatDTO> last30DaysSignups,
                                    List<DailyStatDTO> last30DaysListings) {}

    public record DailyStatDTO(String date, long count) {}

    public record AdminUserListItemDTO(UUID id, String email, String displayName, String plan,
                                       String role, int listingsUsedThisMonth,
                                       OffsetDateTime createdAt, OffsetDateTime updatedAt,
                                       String stripeCustomerId, boolean hasActiveSubscription) {}

    public record AdminUserDetailDTO(UUID id, String email, String displayName, String plan,
                                     String role, int listingsUsedThisMonth, int monthlyLimit,
                                     OffsetDateTime createdAt, OffsetDateTime updatedAt,
                                     String stripeCustomerId, String stripeSubscriptionId,
                                     long totalListingsGenerated, long totalBulkJobs,
                                     long totalMonitoredListings, long totalAudits,
                                     long totalClaudeTokensUsed) {}

    public record AdminTicketDTO(UUID id, UUID userId, String userEmail, String type,
                                 String subject, String description, String status,
                                 String priority, String adminNotes,
                                 OffsetDateTime createdAt, OffsetDateTime updatedAt) {}

    public record AdminAuditLogDTO(UUID id, String adminEmail, String action,
                                   String targetType, UUID targetId, String details,
                                   String ipAddress, OffsetDateTime createdAt) {}

    public record AdminClaudeUsageDTO(long totalCallsToday, long totalCallsThisMonth,
                                      long totalTokensToday, long totalTokensThisMonth,
                                      double estimatedCostToday, double estimatedCostThisMonth,
                                      Map<String, Long> callsByType,
                                      List<TopUserTokenDTO> topUsersByTokens) {}

    public record TopUserTokenDTO(UUID userId, String email, long totalTokens) {}

    public record AdminMonitoringOverviewDTO(long totalMonitoredListings, long activeListings,
                                             long pausedListings, long totalJobsToday,
                                             long failedJobsToday, long pendingJobs,
                                             long pendingAiFixes, double successRateToday) {}

    public record TicketSubmitRequest(@NotBlank @Size(max = 30) String type,
                                      @NotBlank @Size(max = 500) String subject,
                                      @NotBlank @Size(min = 20, max = 2000) String description) {}

    public record TicketUpdateRequest(String status, String priority, String adminNotes) {}

    public record ChangePlanRequest(@NotBlank String plan) {}
}
