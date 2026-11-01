package com.listifyai.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_outbox", uniqueConstraints = @UniqueConstraint(columnNames = "idempotency_key"))
public class EmailOutbox {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "message_type", nullable = false, length = 60) private String messageType;
    @Column(nullable = false, length = 320) private String recipient;
    @Column(nullable = false, length = 500) private String subject;
    @Column(name = "html_body", nullable = false, columnDefinition = "TEXT") private String htmlBody;
    @Column(nullable = false, length = 20) private String status = "PENDING";
    @Column(nullable = false) private int attempts;
    @Column(name = "max_attempts", nullable = false) private int maxAttempts = 6;
    @Column(name = "next_attempt_at", nullable = false) private OffsetDateTime nextAttemptAt;
    @Column(name = "locked_at") private OffsetDateTime lockedAt;
    @Column(name = "locked_by", length = 100) private String lockedBy;
    @Column(name = "sent_at") private OffsetDateTime sentAt;
    @Column(name = "provider_message_id", length = 255) private String providerMessageId;
    @Column(name = "last_error", length = 2000) private String lastError;
    @Column(name = "idempotency_key", nullable = false, length = 255) private String idempotencyKey;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private OffsetDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private OffsetDateTime updatedAt;
    public UUID getId(){return id;} public String getMessageType(){return messageType;} public void setMessageType(String v){messageType=v;}
    public String getRecipient(){return recipient;} public void setRecipient(String v){recipient=v;} public String getSubject(){return subject;} public void setSubject(String v){subject=v;}
    public String getHtmlBody(){return htmlBody;} public void setHtmlBody(String v){htmlBody=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public int getAttempts(){return attempts;} public void setAttempts(int v){attempts=v;} public int getMaxAttempts(){return maxAttempts;} public void setMaxAttempts(int v){maxAttempts=v;}
    public OffsetDateTime getNextAttemptAt(){return nextAttemptAt;} public void setNextAttemptAt(OffsetDateTime v){nextAttemptAt=v;} public OffsetDateTime getLockedAt(){return lockedAt;} public void setLockedAt(OffsetDateTime v){lockedAt=v;}
    public String getLockedBy(){return lockedBy;} public void setLockedBy(String v){lockedBy=v;} public OffsetDateTime getSentAt(){return sentAt;} public void setSentAt(OffsetDateTime v){sentAt=v;}
    public String getProviderMessageId(){return providerMessageId;} public void setProviderMessageId(String v){providerMessageId=v;} public String getLastError(){return lastError;} public void setLastError(String v){lastError=v;}
    public String getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(String v){idempotencyKey=v;}
}
