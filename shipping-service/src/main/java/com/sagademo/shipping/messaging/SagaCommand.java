package com.sagademo.shipping.messaging;

import java.math.BigDecimal;
import java.util.UUID;

public record SagaCommand(
        UUID sagaId,
        String step,
        String targetService,
        String idempotencyKey,
        UUID resourceId,
        UUID customerId,
        BigDecimal amount,
        String itemId,
        Integer quantity,
        String carrier,
        String address,
        boolean simulateFailure
) {
}
