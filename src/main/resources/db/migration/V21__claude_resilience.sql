ALTER TABLE claude_usage_logs
ADD COLUMN prompt_version VARCHAR(30),
ADD COLUMN request_type VARCHAR(50),
ADD COLUMN estimated_cost DECIMAL(10, 6),
ADD COLUMN failure_reason VARCHAR(50);

UPDATE claude_usage_logs SET request_type = call_type WHERE request_type IS NULL;
