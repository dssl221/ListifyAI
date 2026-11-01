package com.listifyai.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "analyzed_listings",
        indexes = {
                @Index(name = "idx_analyzed_listings_user_created", columnList = "user_id, created_at DESC"),
                @Index(name = "idx_analyzed_listings_etsy_id", columnList = "etsy_listing_id")
        }
)
public class AnalyzedListing {

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

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "listing_snapshot", nullable = false, columnDefinition = "jsonb")
    private String listingSnapshot;

    @Column(name = "cache_hit", nullable = false)
    private Boolean cacheHit = false;

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
    public String getListingSnapshot() { return listingSnapshot; }
    public void setListingSnapshot(String listingSnapshot) { this.listingSnapshot = listingSnapshot; }
    public Boolean getCacheHit() { return cacheHit; }
    public void setCacheHit(Boolean cacheHit) { this.cacheHit = cacheHit; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
