package com.sagademo.temporal.workflow;

import java.util.UUID;

public record SagaResult(
        UUID sagaId,
        String status,
        UUID orderId,
        UUID reservationId,
        UUID paymentId,
        UUID shipmentId
) {
}
