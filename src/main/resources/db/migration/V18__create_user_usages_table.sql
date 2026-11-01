CREATE TABLE user_usages (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    used_count INT NOT NULL DEFAULT 0,
    plan_limit INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE (user_id, period_start, period_end)
);

INSERT INTO user_usages (id, user_id, period_start, period_end, used_count, plan_limit)
SELECT gen_random_uuid(), id, date_trunc('month', current_date), (date_trunc('month', current_date) + interval '1 month - 1 day'), listings_used_this_month, 
       CASE plan WHEN 'free' THEN 5 WHEN 'starter' THEN 30 WHEN 'growth' THEN 100 ELSE 2147483647 END
FROM users
ON CONFLICT DO NOTHING;

ALTER TABLE users DROP COLUMN listings_used_this_month;
