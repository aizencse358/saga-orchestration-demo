CREATE TABLE shipments (
    id              UUID PRIMARY KEY,
    saga_id         UUID NOT NULL,
    status          VARCHAR(20) NOT NULL,
    carrier         VARCHAR(255) NOT NULL,
    address         VARCHAR(500) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_shipments_idempotency_key ON shipments (idempotency_key);
CREATE INDEX ix_shipments_saga_id ON shipments (saga_id);
