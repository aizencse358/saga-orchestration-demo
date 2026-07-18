package com.sagademo.temporal.dto;

import java.util.UUID;

public record SagaResponse(
        UUID sagaId,
        String status,
        String currentStep,
        UUID orderId,
        UUID reservationId,
        UUID paymentId,
        UUID shipmentId
) {
}
