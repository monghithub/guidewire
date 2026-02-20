CREATE TABLE incidents (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id      UUID          NOT NULL,
    customer_id   UUID          NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    priority      VARCHAR(10)   NOT NULL DEFAULT 'MEDIUM',
    title         VARCHAR(255)  NOT NULL,
    description   TEXT          NOT NULL,
    assigned_to   VARCHAR(100),
    resolution    TEXT,
    source_event  VARCHAR(255),
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_incidents_claim_id    ON incidents (claim_id);
CREATE INDEX idx_incidents_customer_id ON incidents (customer_id);
CREATE INDEX idx_incidents_status      ON incidents (status);
CREATE INDEX idx_incidents_priority    ON incidents (priority);
CREATE INDEX idx_incidents_created_at  ON incidents (created_at);
