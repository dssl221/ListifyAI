package com.listifyai.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "listings",
    indexes = {
        @Index(name = "idx_listings_user_id", columnList = "user_id"),
        @Index(name = "idx_listings_created",  columnList = "created_at DESC")
    }
)
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "product_name", nullable = false, length = 500)
    private String productName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_input", nullable = false, columnDefinition = "jsonb")
    private String rawInput;

    @Column(name = "platform", nullable = false, length = 20)
    private String platform;

    @Column(name = "tone", nullable = false, length = 30)
    private String tone;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "etsy_output", columnDefinition = "jsonb")
    private String etsyOutput;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shopify_output", columnDefinition = "jsonb")
    private String shopifyOutput;

    @Column(name = "is_saved", nullable = false)
    private Boolean isSaved = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Listing() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    
    public String getRawInput() { return rawInput; }
    public void setRawInput(String rawInput) { this.rawInput = rawInput; }
    
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    
    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }
    
    public String getEtsyOutput() { return etsyOutput; }
    public void setEtsyOutput(String etsyOutput) { this.etsyOutput = etsyOutput; }
    
    public String getShopifyOutput() { return shopifyOutput; }
    public void setShopifyOutput(String shopifyOutput) { this.shopifyOutput = shopifyOutput; }
    
    public Boolean getIsSaved() { return isSaved; }
    public void setIsSaved(Boolean isSaved) { this.isSaved = isSaved; }
    
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
