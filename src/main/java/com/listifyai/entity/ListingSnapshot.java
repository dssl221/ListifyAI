package com.listifyai.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "listing_snapshots",
        indexes = {
                @Index(name = "idx_listing_snapshots_listing_captured", columnList = "monitored_listing_id, captured_at DESC"),
                @Index(name = "idx_listing_snapshots_user_captured", columnList = "user_id, captured_at DESC"),
                @Index(name = "idx_listing_snapshots_checksum", columnList = "checksum")
        }
)
public class ListingSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monitored_listing_id", nullable = false)
    private MonitoredListing monitoredListing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "etsy_listing_id", nullable = false, length = 40)
    private String etsyListingId;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", nullable = false, columnDefinition = "jsonb")
    private String tags;

    @Column(name = "price", length = 80)
    private String price;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "views", nullable = false)
    private Integer views = 0;

    @Column(name = "favorites", nullable = false)
    private Integer favorites = 0;

    @Column(name = "score", nullable = false)
    private Integer score = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "optimization_flags", nullable = false, columnDefinition = "jsonb")
    private String optimizationFlags;

    @Column(name = "checksum", nullable = false, length = 64)
    private String checksum;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_snapshot", nullable = false, columnDefinition = "jsonb")
    private String sourceSnapshot;

    @Column(name = "captured_at", nullable = false)
    private OffsetDateTime capturedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public MonitoredListing getMonitoredListing() { return monitoredListing; }
    public void setMonitoredListing(MonitoredListing monitoredListing) { this.monitoredListing = monitoredListing; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getEtsyListingId() { return etsyListingId; }
    public void setEtsyListingId(String etsyListingId) { this.etsyListingId = etsyListingId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Integer getViews() { return views; }
    public void setViews(Integer views) { this.views = views; }
    public Integer getFavorites() { return favorites; }
    public void setFavorites(Integer favorites) { this.favorites = favorites; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getOptimizationFlags() { return optimizationFlags; }
    public void setOptimizationFlags(String optimizationFlags) { this.optimizationFlags = optimizationFlags; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public String getSourceSnapshot() { return sourceSnapshot; }
    public void setSourceSnapshot(String sourceSnapshot) { this.sourceSnapshot = sourceSnapshot; }
    public OffsetDateTime getCapturedAt() { return capturedAt; }
    public void setCapturedAt(OffsetDateTime capturedAt) { this.capturedAt = capturedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
