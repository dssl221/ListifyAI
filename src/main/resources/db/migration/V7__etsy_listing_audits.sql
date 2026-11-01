CREATE TABLE IF NOT EXISTS analyzed_listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    etsy_listing_id VARCHAR(40) NOT NULL,
    listing_url VARCHAR(1000) NOT NULL,
    title VARCHAR(500) NOT NULL,
    listing_snapshot JSONB NOT NULL,
    cache_hit BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_analyzed_listings_user_created
    ON analyzed_listings(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_analyzed_listings_etsy_id
    ON analyzed_listings(etsy_listing_id);

CREATE TABLE IF NOT EXISTS cached_ai_outputs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    etsy_listing_id VARCHAR(40) NOT NULL,
    section VARCHAR(40) NOT NULL,
    prompt_version VARCHAR(30) NOT NULL,
    input_hash VARCHAR(64) NOT NULL,
    output_json JSONB NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_cached_ai_lookup
    ON cached_ai_outputs(etsy_listing_id, section, prompt_version, input_hash, expires_at);

CREATE TABLE IF NOT EXISTS listing_analysis (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    analyzed_listing_id UUID NOT NULL REFERENCES analyzed_listings(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    score INT NOT NULL CHECK (score >= 0 AND score <= 100),
    audit_json JSONB NOT NULL,
    validation_json JSONB NOT NULL,
    prompt_version VARCHAR(30) NOT NULL,
    cached_output_id UUID REFERENCES cached_ai_outputs(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_listing_analysis_listing
    ON listing_analysis(analyzed_listing_id);
CREATE INDEX IF NOT EXISTS idx_listing_analysis_user_created
    ON listing_analysis(user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS audit_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    period_start DATE NOT NULL,
    reset_date DATE NOT NULL,
    audits_used INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_audit_usage_user_period UNIQUE(user_id, period_start)
);

CREATE TABLE IF NOT EXISTS regeneration_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    analysis_id UUID NOT NULL REFERENCES listing_analysis(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    section VARCHAR(40) NOT NULL,
    previous_value JSONB,
    new_value JSONB NOT NULL,
    reason VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_regeneration_analysis
    ON regeneration_history(analysis_id, created_at DESC);
