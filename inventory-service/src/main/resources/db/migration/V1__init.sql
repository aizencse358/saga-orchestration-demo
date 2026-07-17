CREATE TABLE reservations (
    id              UUID PRIMARY KEY,
    saga_id         UUID NOT NULL,
    status          VARCHAR(20) NOT NULL,
    item_id         VARCHAR(255) NOT NULL,
    quantity        INTEGER NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_reservations_idempotency_key ON reservations (idempotency_key);
CREATE INDEX ix_reservations_saga_id ON reservations (saga_id);
