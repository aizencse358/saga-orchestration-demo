package com.sagademo.temporal.dto;

import java.time.Instant;

public record SagaEventResponse(
        long eventId,
        String eventType,
        Instant timestamp
) {
}
