CREATE TABLE IF NOT EXISTS shop_health_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    overall_score INT NOT NULL CHECK (overall_score >= 0 AND overall_score <= 100),
    listings_analysed INT NOT NULL DEFAULT 0,
    health_json JSONB NOT NULL,
    computed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_shop_health_user UNIQUE (user_id)
);

CREATE INDEX IF NOT EXISTS idx_shop_health_user
    ON shop_health_snapshots(user_id);
