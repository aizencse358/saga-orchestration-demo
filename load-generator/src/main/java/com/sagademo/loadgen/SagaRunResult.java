package com.sagademo.loadgen;

public record SagaRunResult(
        String sagaId,
        String tier,
        String injectedFailureStep,
        long submittedAtEpochMs,
        long completedAtEpochMs,
        long latencyMs,
        SagaOutcome outcome,
        String errorMessage
) {
}
