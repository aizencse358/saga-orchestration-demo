package com.sagademo.orchestrator.dto;

import com.sagademo.orchestrator.model.Saga;

import java.time.Instant;
import java.util.UUID;

public record SagaResponse(
        UUID sagaId,
        String status,
        String direction,
        int stepIndex,
        UUID orderId,
        UUID reservationId,
        UUID paymentId,
        UUID shipmentId,
        Instant createdAt,
        Instant updatedAt
) {
    public static SagaResponse from(Saga saga) {
        return new SagaResponse(
                saga.getId(),
                saga.getStatus().name(),
                saga.getDirection().name(),
                saga.getStepIndex(),
                saga.getOrderId(),
                saga.getReservationId(),
                saga.getPaymentId(),
                saga.getShipmentId(),
                saga.getCreatedAt(),
                saga.getUpdatedAt()
        );
    }
}
