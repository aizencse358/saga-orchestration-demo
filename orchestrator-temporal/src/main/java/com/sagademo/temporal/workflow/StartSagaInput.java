package com.sagademo.temporal.workflow;

import java.math.BigDecimal;
import java.util.UUID;

public record StartSagaInput(
        UUID sagaId,
        UUID customerId,
        BigDecimal amount,
        String itemId,
        int quantity,
        String carrier,
        String address,
        String simulateFailureAtStep
) {
}
