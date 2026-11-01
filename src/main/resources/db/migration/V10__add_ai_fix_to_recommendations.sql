ALTER TABLE monitoring_recommendations
    ADD COLUMN IF NOT EXISTS ai_fix_text TEXT,
    ADD COLUMN IF NOT EXISTS ai_generated_at TIMESTAMPTZ;
