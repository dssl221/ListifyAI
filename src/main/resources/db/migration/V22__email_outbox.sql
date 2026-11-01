CREATE TABLE email_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_type VARCHAR(60) NOT NULL,
    recipient VARCHAR(320) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    html_body TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 6,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    locked_at TIMESTAMPTZ,
    locked_by VARCHAR(100),
    sent_at TIMESTAMPTZ,
    provider_message_id VARCHAR(255),
    last_error VARCHAR(2000),
    idempotency_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_email_outbox_idempotency UNIQUE (idempotency_key)
);
CREATE INDEX idx_email_outbox_claim ON email_outbox(status, next_attempt_at, created_at);
CREATE INDEX idx_email_outbox_processing ON email_outbox(status, locked_at);
