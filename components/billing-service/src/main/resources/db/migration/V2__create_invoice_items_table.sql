CREATE TABLE invoice_items (
    id          UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id  UUID           NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description VARCHAR(255)   NOT NULL,
    quantity    INTEGER        NOT NULL,
    unit_price  DECIMAL(10,2)  NOT NULL,
    subtotal    DECIMAL(10,2)  NOT NULL
);

CREATE INDEX idx_invoice_items_invoice_id ON invoice_items (invoice_id);
