ALTER TABLE monitoring_events
    ADD COLUMN IF NOT EXISTS rule_version VARCHAR(20) NOT NULL DEFAULT '1.0';

ALTER TABLE monitoring_events
    ADD COLUMN IF NOT EXISTS rules_engine_version VARCHAR(20) NOT NULL DEFAULT '1.0';
