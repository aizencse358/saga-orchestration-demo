CREATE TABLE sagas (
    id UUID PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    step_index INT NOT NULL,
    direction VARCHAR(16) NOT NULL,
    customer_id UUID NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    item_id VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    carrier VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    simulate_failure_at_step VARCHAR(64),
    order_id UUID,
    reservation_id UUID,
    payment_id UUID,
    shipment_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE saga_events (
    id BIGSERIAL PRIMARY KEY,
    saga_id UUID NOT NULL,
    step VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    detail TEXT,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_saga_events_saga_id ON saga_events(saga_id);
