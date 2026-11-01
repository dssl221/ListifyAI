package com.listifyai.entity;

import com.listifyai.monitoring.MonitoringEnums.MonitoringSeverity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "monitoring_events",
        indexes = {
                @Index(name = "idx_monitoring_events_user_created", columnList = "user_id, created_at DESC"),
                @Index(name = "idx_monitoring_events_listing_created", columnList = "monitored_listing_id, created_at DESC")
        }
)
public class MonitoringEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitored_listing_id")
    private MonitoredListing monitoredListing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitoring_job_id")
    private MonitoringJob monitoringJob;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 30)
    private MonitoringSeverity severity = MonitoringSeverity.INFO;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private String details;

    @Column(name = "rule_version", nullable = false, length = 20)
    private String ruleVersion = "1.0";

    @Column(name = "rules_engine_version", nullable = false, length = 20)
    private String rulesEngineVersion = "1.0";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public MonitoredListing getMonitoredListing() { return monitoredListing; }
    public void setMonitoredListing(MonitoredListing monitoredListing) { this.monitoredListing = monitoredListing; }
    public MonitoringJob getMonitoringJob() { return monitoringJob; }
    public void setMonitoringJob(MonitoringJob monitoringJob) { this.monitoringJob = monitoringJob; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public MonitoringSeverity getSeverity() { return severity; }
    public void setSeverity(MonitoringSeverity severity) { this.severity = severity; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }
    public String getRulesEngineVersion() { return rulesEngineVersion; }
    public void setRulesEngineVersion(String rulesEngineVersion) { this.rulesEngineVersion = rulesEngineVersion; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
