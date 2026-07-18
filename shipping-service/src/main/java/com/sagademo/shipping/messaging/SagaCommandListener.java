package com.sagademo.shipping.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sagademo.shipping.dto.CreateShipmentRequest;
import com.sagademo.shipping.model.Shipment;
import com.sagademo.shipping.service.ShippingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class SagaCommandListener {

    private static final Logger log = LoggerFactory.getLogger(SagaCommandListener.class);
    private static final String TARGET_SERVICE = "SHIPPING";

    private final ShippingService shippingService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public SagaCommandListener(ShippingService shippingService, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.shippingService = shippingService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "saga.commands", groupId = "shipping-service")
    public void onCommand(String message) {
        SagaCommand command;
        try {
            command = objectMapper.readValue(message, SagaCommand.class);
        } catch (Exception e) {
            log.error("Failed to parse saga command: {}", message, e);
            return;
        }

        if (!TARGET_SERVICE.equals(command.targetService())) {
            return;
        }

        try {
            switch (command.step()) {
                case "CONFIRM_SHIPMENT" -> {
                    Shipment shipment = shippingService.confirm(new CreateShipmentRequest(
                            command.idempotencyKey(), command.sagaId(), command.carrier(),
                            command.address(), command.simulateFailure()));
                    reply(command, "SUCCESS", shipment.getId(), null);
                }
                default -> log.warn("Unknown step for shipping-service: {}", command.step());
            }
        } catch (Exception e) {
            log.warn("Command failed sagaId={} step={}: {}", command.sagaId(), command.step(), e.getMessage());
            reply(command, "FAILURE", null, e.getMessage());
        }
    }

    private void reply(SagaCommand command, String status, java.util.UUID resourceId, String errorMessage) {
        SagaReply sagaReply = new SagaReply(command.sagaId(), command.step(), status, resourceId, errorMessage);
        try {
            kafkaTemplate.send("saga.replies", command.sagaId().toString(), objectMapper.writeValueAsString(sagaReply));
        } catch (Exception e) {
            log.error("Failed to publish saga reply for sagaId={} step={}", command.sagaId(), command.step(), e);
        }
    }
}
