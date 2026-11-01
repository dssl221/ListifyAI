CREATE TABLE IF NOT EXISTS reviews (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    display_name  VARCHAR(120)  NOT NULL,
    seller_role   VARCHAR(120)  NOT NULL,
    rating        INT           NOT NULL CHECK (rating BETWEEN 1 AND 5),
    body          VARCHAR(1000) NOT NULL,
    approved      BOOLEAN       NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_reviews_approved_created
    ON reviews(approved, created_at DESC);
