UPDATE users
SET billing_cycle_start = created_at::date
WHERE billing_cycle_start IS NULL;
