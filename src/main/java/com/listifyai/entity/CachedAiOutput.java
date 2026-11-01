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
        name = "cached_ai_outputs",
        indexes = {
                @Index(name = "idx_cached_ai_lookup", columnList = "etsy_listing_id, marketplace, section, prompt_version, model_version, validator_version, schema_version, cache_key_version, expires_at")
        }
)
public class CachedAiOutput {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "etsy_listing_id", nullable = false, length = 40)
    private String etsyListingId;

    @Column(name = "section", nullable = false, length = 40)
    private String section;

    @Column(name = "prompt_version", nullable = false, length = 30)
    private String promptVersion;

    @Column(name = "marketplace", nullable = false, length = 20)
    private String marketplace;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Column(name = "validator_version", nullable = false, length = 20)
    private String validatorVersion;

    @Column(name = "schema_version", nullable = false, length = 20)
    private String schemaVersion;

    @Column(name = "cache_key_version", nullable = false, length = 20)
    private String cacheKeyVersion;

    @Column(name = "input_hash", nullable = false, length = 64)
    private String inputHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_json", nullable = false, columnDefinition = "jsonb")
    private String outputJson;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEtsyListingId() { return etsyListingId; }
    public void setEtsyListingId(String etsyListingId) { this.etsyListingId = etsyListingId; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    
    public String getMarketplace() { return marketplace; }
    public void setMarketplace(String marketplace) { this.marketplace = marketplace; }
    
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    
    public String getValidatorVersion() { return validatorVersion; }
    public void setValidatorVersion(String validatorVersion) { this.validatorVersion = validatorVersion; }
    
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }
    
    public String getCacheKeyVersion() { return cacheKeyVersion; }
    public void setCacheKeyVersion(String cacheKeyVersion) { this.cacheKeyVersion = cacheKeyVersion; }
    
    public String getInputHash() { return inputHash; }
    public void setInputHash(String inputHash) { this.inputHash = inputHash; }
    public String getOutputJson() { return outputJson; }
    public void setOutputJson(String outputJson) { this.outputJson = outputJson; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
