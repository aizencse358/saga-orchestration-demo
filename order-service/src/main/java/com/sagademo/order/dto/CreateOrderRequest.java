package com.sagademo.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderRequest(
        @NotBlank String idempotencyKey,
        @NotNull UUID sagaId,
        @NotNull UUID customerId,
        @NotNull @Positive BigDecimal amount
) {
}
