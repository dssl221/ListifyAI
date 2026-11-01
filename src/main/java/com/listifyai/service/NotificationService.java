package com.listifyai.service;

import com.listifyai.entity.MonitoringEvent;
import com.listifyai.entity.SupportTicket;
import com.listifyai.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final EmailOutboxService outbox;
    private final String frontendUrl;
    private final String adminEmail;

    public NotificationService(EmailOutboxService outbox,
                               @Value("${frontend.url}") String frontendUrl,
                               @Value("${admin.email:}") String adminEmail) {
        this.outbox = outbox;
        this.frontendUrl = frontendUrl;
        this.adminEmail = adminEmail;
    }

    public void sendMonitoringAlert(User user, MonitoringEvent event) {
        if (user.getEmail() == null || user.getEmail().isBlank()) return;
        String listingTitle = event.getMonitoredListing() == null || event.getMonitoredListing().getTitle() == null
                ? "Your monitored listing"
                : event.getMonitoredListing().getTitle();
        String dashboardUrl = frontendUrl + "/dashboard/monitoring";
        String html = """
                <h2>Listing monitoring alert</h2>
                <p><strong>Listing:</strong> %s</p>
                <p><strong>Issue:</strong> %s</p>
                <p><strong>Recommendation:</strong> %s</p>
                <p><a href="%s" style="background:#111827;color:#ffffff;padding:12px 16px;text-decoration:none;border-radius:6px;">Review monitoring dashboard</a></p>
                """.formatted(escape(listingTitle), escape(event.getMessage()), escape(summary(event)), dashboardUrl);

        outbox.enqueue("MONITORING_ALERT", user.getEmail(), "TagWing monitoring alert", html, "monitoring-alert:" + event.getId());
    }

    public void sendEmail(User user, String type, String subject, String html, String idempotencyKey) {
        if (user.getEmail() != null && !user.getEmail().isBlank()) outbox.enqueue(type, user.getEmail(), subject, html, idempotencyKey);
    }

    public void sendAdminAlert(String subject, String body) {
        if (adminEmail != null && !adminEmail.isBlank()) outbox.enqueue("ADMIN_ALERT", adminEmail, subject, "<pre style=\"font-family:ui-monospace,monospace;white-space:pre-wrap;\">" + escape(body) + "</pre>", "admin-alert:" + Integer.toHexString((subject+body).hashCode()));
    }

    public void sendSupportTicketCreated(SupportTicket ticket) {
        if (adminEmail == null || adminEmail.isBlank()) return;
        User user = ticket.getUser();
        String body = "From: " + user.getEmail() + "\nPlan: " + user.getPlan()
                + "\nType: " + ticket.getType() + "\n\n" + ticket.getDescription();
        outbox.enqueue("SUPPORT_TICKET_CREATED", adminEmail, "New support ticket: " + ticket.getSubject(),
                "<pre style=\"font-family:ui-monospace,monospace;white-space:pre-wrap;\">" + escape(body) + "</pre>",
                "support-ticket:" + ticket.getId() + ":created");
    }

    private String summary(MonitoringEvent event) {
        return event.getDetails() == null || event.getDetails().isBlank()
                ? event.getMessage()
                : event.getDetails();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
