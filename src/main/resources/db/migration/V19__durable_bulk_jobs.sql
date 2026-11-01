ALTER TABLE bulk_jobs RENAME COLUMN total_rows TO total_items;
ALTER TABLE bulk_jobs RENAME COLUMN processed_rows TO completed_items;
ALTER TABLE bulk_jobs RENAME COLUMN failed_rows TO failed_items;

ALTER TABLE bulk_jobs ADD COLUMN started_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE bulk_jobs ADD COLUMN retry_count INT NOT NULL DEFAULT 0;
ALTER TABLE bulk_jobs ADD COLUMN idempotency_key VARCHAR(255);

UPDATE bulk_jobs SET status = 'PENDING' WHERE status = 'queued';
UPDATE bulk_jobs SET status = 'RUNNING' WHERE status = 'processing';
UPDATE bulk_jobs SET status = 'COMPLETED' WHERE status = 'done';
UPDATE bulk_jobs SET status = 'FAILED' WHERE status = 'failed';

CREATE TABLE bulk_job_items (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES bulk_jobs(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    input_payload TEXT NOT NULL,
    result_reference TEXT,
    attempt_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    locked_by VARCHAR(255),
    locked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_bulk_job_items_status ON bulk_job_items(status);
CREATE INDEX idx_bulk_job_items_job_id ON bulk_job_items(job_id);
