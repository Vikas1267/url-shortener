CREATE TABLE clicks (
    id            BIGSERIAL PRIMARY KEY,
    url_id        BIGINT       NOT NULL REFERENCES urls(id) ON DELETE CASCADE,
    clicked_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    referrer      TEXT,
    ip_address    INET,
    user_agent    TEXT
);

-- Rationale: backs the foreign key and supports COUNT(*) WHERE url_id = ? for total-clicks.
CREATE INDEX idx_clicks_url_id ON clicks(url_id);

-- Rationale: stats filter by url_id and group/range by clicked_at, so this avoids a broad scan.
CREATE INDEX idx_clicks_url_id_clicked_at ON clicks(url_id, clicked_at);
