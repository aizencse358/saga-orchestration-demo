package com.sagademo.orchestrator.dto;

import com.sagademo.orchestrator.model.SagaEvent;

import java.time.Instant;

public record SagaEventResponse(
        String step,
        String status,
        String detail,
        Instant createdAt
) {
    public static SagaEventResponse from(SagaEvent event) {
        return new SagaEventResponse(event.getStep(), event.getStatus(), event.getDetail(), event.getCreatedAt());
    }
}
