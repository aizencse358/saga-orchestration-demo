package com.sagademo.orchestrator.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sagademo.orchestrator.engine.SagaOrchestratorEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SagaReplyListener {

    private static final Logger log = LoggerFactory.getLogger(SagaReplyListener.class);

    private final SagaOrchestratorEngine engine;
    private final ObjectMapper objectMapper;

    public SagaReplyListener(SagaOrchestratorEngine engine, ObjectMapper objectMapper) {
        this.engine = engine;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "saga.replies", groupId = "orchestrator")
    public void onReply(String message) {
        try {
            SagaReply reply = objectMapper.readValue(message, SagaReply.class);
            engine.handleReply(reply);
        } catch (Exception e) {
            log.error("Failed to process saga reply: {}", message, e);
        }
    }
}
