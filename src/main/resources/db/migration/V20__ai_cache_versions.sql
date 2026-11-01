ALTER TABLE cached_ai_outputs
ADD COLUMN marketplace VARCHAR(20) DEFAULT 'ETSY' NOT NULL,
ADD COLUMN model_version VARCHAR(50) DEFAULT 'unknown' NOT NULL,
ADD COLUMN validator_version VARCHAR(20) DEFAULT 'v1.0' NOT NULL,
ADD COLUMN schema_version VARCHAR(20) DEFAULT 'v1.0' NOT NULL,
ADD COLUMN cache_key_version VARCHAR(20) DEFAULT 'v1.0' NOT NULL;

DROP INDEX idx_cached_ai_lookup;

CREATE INDEX idx_cached_ai_lookup
ON cached_ai_outputs(etsy_listing_id, marketplace, section, prompt_version, model_version, validator_version, schema_version, cache_key_version, expires_at);
