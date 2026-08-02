CREATE TABLE urls (
    id            BIGSERIAL PRIMARY KEY,
    short_code    VARCHAR(10)  NOT NULL,
    long_url      TEXT         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at    TIMESTAMPTZ,
    is_active     BOOLEAN      NOT NULL DEFAULT true,
    CONSTRAINT uq_short_code UNIQUE (short_code)
);

-- Rationale: every redirect hits this lookup, making it the hottest read path.
-- The unique index is also the final database guard against any duplicate short_code.
CREATE UNIQUE INDEX idx_urls_short_code ON urls(short_code);
