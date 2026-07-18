package com.sagademo.orchestrator.model;

public enum SagaStatus {
    IN_PROGRESS,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED
}
