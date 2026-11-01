ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20)
    NOT NULL DEFAULT 'user';
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
-- To set yourself as admin after deployment:
-- UPDATE users SET role = 'admin' WHERE email = 'your@email.com';

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_user_id UUID         NOT NULL REFERENCES users(id)
                               ON DELETE CASCADE,
    action        VARCHAR(100) NOT NULL,
    target_type   VARCHAR(50),
    target_id     UUID,
    details       JSONB,
    ip_address    VARCHAR(45),
    user_agent    VARCHAR(500),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_admin_audit_admin_user
    ON admin_audit_logs(admin_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admin_audit_action
    ON admin_audit_logs(action, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admin_audit_target
    ON admin_audit_logs(target_type, target_id);

CREATE TABLE IF NOT EXISTS support_tickets (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES users(id)
                              ON DELETE CASCADE,
    type         VARCHAR(30)  NOT NULL,
    subject      VARCHAR(500) NOT NULL,
    description  TEXT         NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'open',
    priority     VARCHAR(10)  NOT NULL DEFAULT 'normal',
    admin_notes  TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_support_tickets_user
    ON support_tickets(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_support_tickets_status
    ON support_tickets(status, priority, created_at DESC);

CREATE TABLE IF NOT EXISTS claude_usage_logs (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID        REFERENCES users(id) ON DELETE SET NULL,
    call_type      VARCHAR(50) NOT NULL,
    input_tokens   INT         NOT NULL DEFAULT 0,
    output_tokens  INT         NOT NULL DEFAULT 0,
    model          VARCHAR(80) NOT NULL,
    latency_ms     INT,
    success        BOOLEAN     NOT NULL DEFAULT true,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_claude_usage_user_created
    ON claude_usage_logs(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_claude_usage_created
    ON claude_usage_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_claude_usage_call_type
    ON claude_usage_logs(call_type, created_at DESC);
