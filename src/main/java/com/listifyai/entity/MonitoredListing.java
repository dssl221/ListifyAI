package com.listifyai.entity;

import com.listifyai.monitoring.MonitoringEnums.MonitoredListingStatus;
import com.listifyai.monitoring.MonitoringEnums.OptimizationState;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "monitored_listings",
        uniqueConstraints = @UniqueConstraint(name = "uk_monitored_listing_user_etsy", columnNames = {"user_id", "etsy_listing_id"}),
        indexes = {
                @Index(name = "idx_monitored_listings_due", columnList = "monitoring_enabled, status, next_check_at"),
                @Index(name = "idx_monitored_listings_user_status", columnList = "user_id, status, created_at DESC")
        }
)
public class MonitoredListing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "etsy_listing_id", nullable = false, length = 40)
    private String etsyListingId;

    @Column(name = "listing_url", nullable = false, length = 1000)
    private String listingUrl;

    @Column(name = "title", length = 500)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MonitoredListingStatus status = MonitoredListingStatus.ACTIVE;

    @Column(name = "monitoring_enabled", nullable = false)
    private Boolean monitoringEnabled = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_snapshot_id")
    private ListingSnapshot lastSnapshot;

    @Column(name = "last_snapshot_at")
    private OffsetDateTime lastSnapshotAt;

    @Column(name = "last_score")
    private Integer lastScore;

    @Column(name = "last_checked_at")
    private OffsetDateTime lastCheckedAt;

    @Column(name = "next_check_at", nullable = false)
    private OffsetDateTime nextCheckAt;

    @Column(name = "last_job_id")
    private UUID lastJobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "optimization_state", nullable = false, length = 40)
    private OptimizationState optimizationState = OptimizationState.NEVER_OPTIMIZED;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Version
    private Long version;

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
    public String getEtsyListingId() { return etsyListingId; }
    public void setEtsyListingId(String etsyListingId) { this.etsyListingId = etsyListingId; }
    public String getListingUrl() { return listingUrl; }
    public void setListingUrl(String listingUrl) { this.listingUrl = listingUrl; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public MonitoredListingStatus getStatus() { return status; }
    public void setStatus(MonitoredListingStatus status) { this.status = status; }
    public Boolean getMonitoringEnabled() { return monitoringEnabled; }
    public void setMonitoringEnabled(Boolean monitoringEnabled) { this.monitoringEnabled = monitoringEnabled; }
    public ListingSnapshot getLastSnapshot() { return lastSnapshot; }
    public void setLastSnapshot(ListingSnapshot lastSnapshot) { this.lastSnapshot = lastSnapshot; }
    public OffsetDateTime getLastSnapshotAt() { return lastSnapshotAt; }
    public void setLastSnapshotAt(OffsetDateTime lastSnapshotAt) { this.lastSnapshotAt = lastSnapshotAt; }
    public Integer getLastScore() { return lastScore; }
    public void setLastScore(Integer lastScore) { this.lastScore = lastScore; }
    public OffsetDateTime getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(OffsetDateTime lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }
    public OffsetDateTime getNextCheckAt() { return nextCheckAt; }
    public void setNextCheckAt(OffsetDateTime nextCheckAt) { this.nextCheckAt = nextCheckAt; }
    public UUID getLastJobId() { return lastJobId; }
    public void setLastJobId(UUID lastJobId) { this.lastJobId = lastJobId; }
    public OptimizationState getOptimizationState() { return optimizationState; }
    public void setOptimizationState(OptimizationState optimizationState) { this.optimizationState = optimizationState; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
