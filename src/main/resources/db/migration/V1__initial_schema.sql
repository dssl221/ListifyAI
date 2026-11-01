-- ─────────────────────────────────────────────────────────────────────────────
-- ListifyAI  —  PostgreSQL Schema
-- ─────────────────────────────────────────────────────────────────────────────

-- Enable pgcrypto for gen_random_uuid() if not already available
-- (Postgres 13+ ships gen_random_uuid() natively; remove if using older ver)
-- CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ─── users ───────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS users (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email                   VARCHAR(255) UNIQUE NOT NULL,
    supabase_id             VARCHAR(255) UNIQUE NOT NULL,
    plan                    VARCHAR(20)  NOT NULL DEFAULT 'free',   -- free|starter|growth|pro
    stripe_customer_id      VARCHAR(100),
    stripe_subscription_id  VARCHAR(100),
    listings_used_this_month INT         NOT NULL DEFAULT 0,
    billing_cycle_start     DATE,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ─── listings ────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS listings (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_name  VARCHAR(500) NOT NULL,
    raw_input     JSONB        NOT NULL,
    platform      VARCHAR(20)  NOT NULL,   -- etsy|shopify|both
    tone          VARCHAR(30)  NOT NULL,
    etsy_output   JSONB,
    shopify_output JSONB,
    is_saved      BOOLEAN      NOT NULL DEFAULT false,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_listings_user_id ON listings(user_id);
CREATE INDEX IF NOT EXISTS idx_listings_created  ON listings(created_at DESC);

-- ─── bulk_jobs ────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS bulk_jobs (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status         VARCHAR(20) NOT NULL DEFAULT 'queued',   -- queued|processing|done|failed
    total_rows     INT         NOT NULL,
    processed_rows INT         NOT NULL DEFAULT 0,
    failed_rows    INT         NOT NULL DEFAULT 0,
    result_csv_url TEXT,
    error_log      TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at   TIMESTAMPTZ
);
