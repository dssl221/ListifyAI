package com.listifyai.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "listing_analysis", indexes = {
        @Index(name = "idx_listing_analysis_listing", columnList = "analyzed_listing_id"),
        @Index(name = "idx_listing_analysis_user_created", columnList = "user_id, created_at DESC")
})
public class ListingAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analyzed_listing_id", nullable = false)
    private AnalyzedListing analyzedListing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "score", nullable = false)
    private Integer score;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "audit_json", nullable = false, columnDefinition = "jsonb")
    private String auditJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_json", nullable = false, columnDefinition = "jsonb")
    private String validationJson;

    @Column(name = "prompt_version", nullable = false, length = 30)
    private String promptVersion;

    @Column(name = "cached_output_id")
    private UUID cachedOutputId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public AnalyzedListing getAnalyzedListing() { return analyzedListing; }
    public void setAnalyzedListing(AnalyzedListing analyzedListing) { this.analyzedListing = analyzedListing; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getAuditJson() { return auditJson; }
    public void setAuditJson(String auditJson) { this.auditJson = auditJson; }
    public String getValidationJson() { return validationJson; }
    public void setValidationJson(String validationJson) { this.validationJson = validationJson; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public UUID getCachedOutputId() { return cachedOutputId; }
    public void setCachedOutputId(UUID cachedOutputId) { this.cachedOutputId = cachedOutputId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
