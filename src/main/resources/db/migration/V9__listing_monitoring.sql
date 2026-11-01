CREATE TABLE IF NOT EXISTS monitored_listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    etsy_listing_id VARCHAR(40) NOT NULL,
    listing_url VARCHAR(1000) NOT NULL,
    title VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    monitoring_enabled BOOLEAN NOT NULL DEFAULT true,
    last_snapshot_id UUID,
    last_snapshot_at TIMESTAMPTZ,
    last_score INT,
    last_checked_at TIMESTAMPTZ,
    next_check_at TIMESTAMPTZ NOT NULL,
    last_job_id UUID,
    optimization_state VARCHAR(40) NOT NULL DEFAULT 'NEVER_OPTIMIZED',
    metadata JSONB,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_monitored_listing_user_etsy UNIQUE(user_id, etsy_listing_id)
);

CREATE INDEX IF NOT EXISTS idx_monitored_listings_due
    ON monitored_listings(monitoring_enabled, status, next_check_at);
CREATE INDEX IF NOT EXISTS idx_monitored_listings_user_status
    ON monitored_listings(user_id, status, created_at DESC);

CREATE TABLE IF NOT EXISTS listing_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    monitored_listing_id UUID NOT NULL REFERENCES monitored_listings(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    etsy_listing_id VARCHAR(40) NOT NULL,
    title VARCHAR(500),
    description TEXT,
    tags JSONB NOT NULL,
    price VARCHAR(80),
    currency VARCHAR(10),
    views INT NOT NULL DEFAULT 0,
    favorites INT NOT NULL DEFAULT 0,
    score INT NOT NULL CHECK (score >= 0 AND score <= 100),
    optimization_flags JSONB NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    source_snapshot JSONB NOT NULL,
    captured_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_listing_snapshots_listing_captured
    ON listing_snapshots(monitored_listing_id, captured_at DESC);
CREATE INDEX IF NOT EXISTS idx_listing_snapshots_user_captured
    ON listing_snapshots(user_id, captured_at DESC);
CREATE INDEX IF NOT EXISTS idx_listing_snapshots_checksum
    ON listing_snapshots(checksum);

ALTER TABLE monitored_listings
    ADD CONSTRAINT fk_monitored_listing_last_snapshot
    FOREIGN KEY (last_snapshot_id) REFERENCES listing_snapshots(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS monitoring_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    monitored_listing_id UUID NOT NULL REFERENCES monitored_listings(id) ON DELETE CASCADE,
    job_type VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    priority INT NOT NULL DEFAULT 100,
    idempotency_key VARCHAR(160) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,
    scheduled_at TIMESTAMPTZ NOT NULL,
    available_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    last_error TEXT,
    payload JSONB,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_monitoring_job_idempotency UNIQUE(idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_monitoring_jobs_status_available
    ON monitoring_jobs(status, available_at, priority);
CREATE INDEX IF NOT EXISTS idx_monitoring_jobs_listing_created
    ON monitoring_jobs(monitored_listing_id, created_at DESC);

CREATE TABLE IF NOT EXISTS monitoring_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    monitored_listing_id UUID REFERENCES monitored_listings(id) ON DELETE CASCADE,
    monitoring_job_id UUID REFERENCES monitoring_jobs(id) ON DELETE SET NULL,
    event_type VARCHAR(80) NOT NULL,
    severity VARCHAR(30) NOT NULL,
    message TEXT NOT NULL,
    details JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_monitoring_events_user_created
    ON monitoring_events(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_monitoring_events_listing_created
    ON monitoring_events(monitored_listing_id, created_at DESC);

CREATE TABLE IF NOT EXISTS monitoring_recommendations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    monitored_listing_id UUID NOT NULL REFERENCES monitored_listings(id) ON DELETE CASCADE,
    snapshot_id UUID REFERENCES listing_snapshots(id) ON DELETE SET NULL,
    recommendation_type VARCHAR(80) NOT NULL,
    status VARCHAR(30) NOT NULL,
    severity VARCHAR(30) NOT NULL,
    opportunity_score INT NOT NULL DEFAULT 0,
    title VARCHAR(300) NOT NULL,
    message TEXT NOT NULL,
    details JSONB,
    ai_candidate BOOLEAN NOT NULL DEFAULT false,
    dedupe_key VARCHAR(180) NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_monitoring_recommendation_dedupe UNIQUE(dedupe_key)
);

CREATE INDEX IF NOT EXISTS idx_monitoring_recommendations_user_status
    ON monitoring_recommendations(user_id, status, detected_at DESC);
CREATE INDEX IF NOT EXISTS idx_monitoring_recommendations_listing_status
    ON monitoring_recommendations(monitored_listing_id, status, detected_at DESC);
