package com.sagademo.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CreateReservationRequest(
        @NotBlank String idempotencyKey,
        @NotNull UUID sagaId,
        @NotBlank String itemId,
        @Positive int quantity,
        Boolean simulateFailure
) {
}
