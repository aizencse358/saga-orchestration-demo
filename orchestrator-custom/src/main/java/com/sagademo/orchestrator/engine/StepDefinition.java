package com.sagademo.orchestrator.engine;

public record StepDefinition(String step, String targetService, String compensationStep) {
}
