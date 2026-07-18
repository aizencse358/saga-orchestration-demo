package com.sagademo.shipping.messaging;

import java.util.UUID;

public record SagaReply(
        UUID sagaId,
        String step,
        String status,
        UUID resourceId,
        String errorMessage
) {
}
