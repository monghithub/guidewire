CREATE TABLE invoices (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id     UUID         NOT NULL,
    customer_id   UUID         NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    total_amount  DECIMAL(10,2) NOT NULL,
    currency      VARCHAR(3)   DEFAULT 'MXN',
    source_event  VARCHAR(255),
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invoices_policy_id   ON invoices (policy_id);
CREATE INDEX idx_invoices_customer_id ON invoices (customer_id);
CREATE INDEX idx_invoices_status      ON invoices (status);
CREATE INDEX idx_invoices_created_at  ON invoices (created_at);
