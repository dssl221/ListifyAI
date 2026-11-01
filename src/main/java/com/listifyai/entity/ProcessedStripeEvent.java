package com.listifyai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "processed_stripe_events")
public class ProcessedStripeEvent {

    @Id
    @Column(name = "event_id", nullable = false, length = 255)
    private String eventId;

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private OffsetDateTime processedAt;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    public ProcessedStripeEvent() {}

    public ProcessedStripeEvent(String eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public OffsetDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(OffsetDateTime processedAt) { this.processedAt = processedAt; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
}
