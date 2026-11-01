package com.listifyai.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bulk_job_items")
public class BulkJobItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private BulkJob job;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, RUNNING, COMPLETED, FAILED, RETRYING, DEAD_LETTER, CANCELLED

    @Column(name = "input_payload", nullable = false, columnDefinition = "TEXT")
    private String inputPayload;

    @Column(name = "result_reference", columnDefinition = "TEXT")
    private String resultReference;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "locked_by", length = 255)
    private String lockedBy;

    @Column(name = "locked_at")
    private OffsetDateTime lockedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public BulkJobItem() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public BulkJob getJob() { return job; }
    public void setJob(BulkJob job) { this.job = job; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getInputPayload() { return inputPayload; }
    public void setInputPayload(String inputPayload) { this.inputPayload = inputPayload; }

    public String getResultReference() { return resultReference; }
    public void setResultReference(String resultReference) { this.resultReference = resultReference; }

    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }

    public OffsetDateTime getLockedAt() { return lockedAt; }
    public void setLockedAt(OffsetDateTime lockedAt) { this.lockedAt = lockedAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
