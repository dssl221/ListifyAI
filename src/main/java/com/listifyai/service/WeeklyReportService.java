package com.listifyai.service;

import com.listifyai.entity.MonitoringEvent;
import com.listifyai.entity.ShopHealthSnapshot;
import com.listifyai.entity.User;
import com.listifyai.repository.MonitoredListingRepository;
import com.listifyai.repository.MonitoringEventRepository;
import com.listifyai.repository.ShopHealthSnapshotRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@SuppressWarnings({"null", "unchecked", "unused"})
public class WeeklyReportService {

    private final MonitoredListingRepository monitoredListingRepository;
    private final MonitoringEventRepository eventRepository;
    private final ShopHealthSnapshotRepository shopHealthSnapshotRepository;
    private final NotificationService notificationService;
    private final String frontendUrl;

    public WeeklyReportService(MonitoredListingRepository monitoredListingRepository,
                               MonitoringEventRepository eventRepository,
                               ShopHealthSnapshotRepository shopHealthSnapshotRepository,
                               NotificationService notificationService,
                               @Value("${frontend.url}") String frontendUrl) {
        this.monitoredListingRepository = monitoredListingRepository;
        this.eventRepository = eventRepository;
        this.shopHealthSnapshotRepository = shopHealthSnapshotRepository;
        this.notificationService = notificationService;
        this.frontendUrl = frontendUrl;
    }

    public void sendWeeklyReport(User user) {
        long monitoredCount = monitoredListingRepository.countByUserId(user.getId());
        List<MonitoringEvent> events = eventRepository.findByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                user.getId(),
                OffsetDateTime.now().minusDays(7)
        );
        int score = shopHealthSnapshotRepository.findFirstByUserIdOrderByComputedAtDesc(user.getId())
                .map(ShopHealthSnapshot::getOverallScore)
                .orElse(0);
        String topRecommendation = events.stream()
                .max(Comparator.comparing(MonitoringEvent::getCreatedAt))
                .map(MonitoringEvent::getMessage)
                .orElse("No new recommendations this week.");
        String html = """
                <h2>Your weekly ListifyAI report</h2>
                <p><strong>Shop health score:</strong> %d</p>
                <p><strong>Monitored listings:</strong> %d</p>
                <p><strong>Issues this week:</strong> %d</p>
                <p><strong>Top recommendation:</strong> %s</p>
                <p><a href="%s/dashboard/monitoring" style="background:#111827;color:#ffffff;padding:12px 16px;text-decoration:none;border-radius:6px;">Open dashboard</a></p>
                """.formatted(score, monitoredCount, events.size(), escape(topRecommendation), frontendUrl);
        String week = java.time.LocalDate.now(java.time.ZoneOffset.UTC).with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).toString();
        notificationService.sendEmail(user, "WEEKLY_SELLER_REPORT", "Your weekly ListifyAI report", html, "weekly-report:" + user.getId() + ":" + week);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
