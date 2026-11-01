package com.listifyai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "shop_health_snapshots")
public class ShopHealthSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "overall_score", nullable = false)
    private int overallScore;

    @Column(name = "listings_analysed", nullable = false)
    private int listingsAnalysed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "health_json", nullable = false, columnDefinition = "jsonb")
    private String healthJson;

    @Column(name = "computed_at", nullable = false)
    private OffsetDateTime computedAt;

    public ShopHealthSnapshot() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public int getOverallScore() { return overallScore; }
    public void setOverallScore(int overallScore) { this.overallScore = overallScore; }

    public int getListingsAnalysed() { return listingsAnalysed; }
    public void setListingsAnalysed(int listingsAnalysed) { this.listingsAnalysed = listingsAnalysed; }

    public String getHealthJson() { return healthJson; }
    public void setHealthJson(String healthJson) { this.healthJson = healthJson; }

    public OffsetDateTime getComputedAt() { return computedAt; }
    public void setComputedAt(OffsetDateTime computedAt) { this.computedAt = computedAt; }
}
