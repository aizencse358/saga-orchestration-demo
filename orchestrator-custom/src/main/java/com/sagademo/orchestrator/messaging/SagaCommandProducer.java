package com.sagademo.orchestrator.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class SagaCommandProducer {

    private static final Logger log = LoggerFactory.getLogger(SagaCommandProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public SagaCommandProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void send(SagaCommand command) {
        try {
            kafkaTemplate.send("saga.commands", command.sagaId().toString(), objectMapper.writeValueAsString(command));
        } catch (Exception e) {
            log.error("Failed to publish saga command sagaId={} step={}", command.sagaId(), command.step(), e);
            throw new RuntimeException("Failed to publish saga command", e);
        }
    }
}
