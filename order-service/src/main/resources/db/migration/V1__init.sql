CREATE TABLE orders (
    id              UUID PRIMARY KEY,
    saga_id         UUID NOT NULL,
    status          VARCHAR(20) NOT NULL,
    customer_id     UUID NOT NULL,
    amount          NUMERIC(19, 2) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_orders_idempotency_key ON orders (idempotency_key);
CREATE INDEX ix_orders_saga_id ON orders (saga_id);
