package com.sagademo.orchestrator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record StartSagaRequest(
        @NotNull UUID customerId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String itemId,
        @Positive int quantity,
        @NotBlank String carrier,
        @NotBlank String address,
        String simulateFailureAtStep
) {
}
