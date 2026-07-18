package com.sagademo.temporal.workflow;

import java.util.UUID;

public record SagaStatusView(
        String currentStep,
        String status,
        UUID orderId,
        UUID reservationId,
        UUID paymentId,
        UUID shipmentId
) {
}
