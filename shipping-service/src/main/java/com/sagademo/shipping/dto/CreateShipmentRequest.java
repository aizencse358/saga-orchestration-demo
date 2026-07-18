package com.sagademo.shipping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateShipmentRequest(
        @NotBlank String idempotencyKey,
        @NotNull UUID sagaId,
        @NotBlank String carrier,
        @NotBlank String address,
        Boolean simulateFailure
) {
}
