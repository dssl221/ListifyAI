package com.listifyai.entity;

import com.listifyai.monitoring.MonitoringEnums.MonitoringRecommendationStatus;
import com.listifyai.monitoring.MonitoringEnums.MonitoringSeverity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "monitoring_recommendations",
        uniqueConstraints = @UniqueConstraint(name = "uk_monitoring_recommendation_dedupe", columnNames = "dedupe_key"),
        indexes = {
                @Index(name = "idx_monitoring_recommendations_user_status", columnList = "user_id, status, detected_at DESC"),
                @Index(name = "idx_monitoring_recommendations_listing_status", columnList = "monitored_listing_id, status, detected_at DESC")
        }
)
public class MonitoringRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monitored_listing_id", nullable = false)
    private MonitoredListing monitoredListing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id")
    private ListingSnapshot snapshot;

    @Column(name = "recommendation_type", nullable = false, length = 80)
    private String recommendationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MonitoringRecommendationStatus status = MonitoringRecommendationStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 30)
    private MonitoringSeverity severity = MonitoringSeverity.LOW;

    @Column(name = "opportunity_score", nullable = false)
    private Integer opportunityScore = 0;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private String details;

    @Column(name = "ai_candidate", nullable = false)
    private Boolean aiCandidate = false;

    @Column(name = "ai_fix_text", columnDefinition = "TEXT")
    private String aiFixText;

    @Column(name = "ai_generated_at")
    private OffsetDateTime aiGeneratedAt;

    @Column(name = "dedupe_key", nullable = false, length = 180)
    private String dedupeKey;

    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public MonitoredListing getMonitoredListing() { return monitoredListing; }
    public void setMonitoredListing(MonitoredListing monitoredListing) { this.monitoredListing = monitoredListing; }
    public ListingSnapshot getSnapshot() { return snapshot; }
    public void setSnapshot(ListingSnapshot snapshot) { this.snapshot = snapshot; }
    public String getRecommendationType() { return recommendationType; }
    public void setRecommendationType(String recommendationType) { this.recommendationType = recommendationType; }
    public MonitoringRecommendationStatus getStatus() { return status; }
    public void setStatus(MonitoringRecommendationStatus status) { this.status = status; }
    public MonitoringSeverity getSeverity() { return severity; }
    public void setSeverity(MonitoringSeverity severity) { this.severity = severity; }
    public Integer getOpportunityScore() { return opportunityScore; }
    public void setOpportunityScore(Integer opportunityScore) { this.opportunityScore = opportunityScore; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public Boolean getAiCandidate() { return aiCandidate; }
    public void setAiCandidate(Boolean aiCandidate) { this.aiCandidate = aiCandidate; }
    public String getAiFixText() { return aiFixText; }
    public void setAiFixText(String aiFixText) { this.aiFixText = aiFixText; }
    public OffsetDateTime getAiGeneratedAt() { return aiGeneratedAt; }
    public void setAiGeneratedAt(OffsetDateTime aiGeneratedAt) { this.aiGeneratedAt = aiGeneratedAt; }
    public String getDedupeKey() { return dedupeKey; }
    public void setDedupeKey(String dedupeKey) { this.dedupeKey = dedupeKey; }
    public OffsetDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(OffsetDateTime detectedAt) { this.detectedAt = detectedAt; }
    public OffsetDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(OffsetDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
