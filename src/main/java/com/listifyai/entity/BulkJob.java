package com.listifyai.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bulk_jobs")
public class BulkJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "total_items", nullable = false)
    private Integer totalItems;

    @Column(name = "completed_items", nullable = false)
    private Integer completedItems = 0;

    @Column(name = "failed_items", nullable = false)
    private Integer failedItems = 0;

    @Column(name = "result_csv_url", columnDefinition = "TEXT")
    private String resultCsvUrl;

    @Column(name = "error_log", columnDefinition = "TEXT")
    private String errorLog;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    public BulkJob() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Integer getTotalItems() { return totalItems; }
    public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }
    
    public Integer getCompletedItems() { return completedItems; }
    public void setCompletedItems(Integer completedItems) { this.completedItems = completedItems; }
    
    public Integer getFailedItems() { return failedItems; }
    public void setFailedItems(Integer failedItems) { this.failedItems = failedItems; }
    
    public String getResultCsvUrl() { return resultCsvUrl; }
    public void setResultCsvUrl(String resultCsvUrl) { this.resultCsvUrl = resultCsvUrl; }
    
    public String getErrorLog() { return errorLog; }
    public void setErrorLog(String errorLog) { this.errorLog = errorLog; }
    
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
