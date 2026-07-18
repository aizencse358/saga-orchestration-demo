package com.sagademo.inventory.messaging;

import java.util.UUID;

public record SagaReply(
        UUID sagaId,
        String step,
        String status,
        UUID resourceId,
        String errorMessage
) {
}
