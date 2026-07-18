package com.sagademo.order.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sagademo.order.dto.CreateOrderRequest;
import com.sagademo.order.model.Order;
import com.sagademo.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class SagaCommandListener {

    private static final Logger log = LoggerFactory.getLogger(SagaCommandListener.class);
    private static final String TARGET_SERVICE = "ORDER";

    private final OrderService orderService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public SagaCommandListener(OrderService orderService, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "saga.commands", groupId = "order-service")
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
                case "CREATE_ORDER" -> {
                    Order order = orderService.create(new CreateOrderRequest(
                            command.idempotencyKey(), command.sagaId(), command.customerId(),
                            command.amount(), command.simulateFailure()));
                    reply(command, "SUCCESS", order.getId(), null);
                }
                case "CANCEL_ORDER" -> {
                    Order order = orderService.cancel(command.resourceId());
                    reply(command, "SUCCESS", order.getId(), null);
                }
                case "COMPLETE_ORDER" -> {
                    Order order = orderService.confirm(command.resourceId());
                    reply(command, "SUCCESS", order.getId(), null);
                }
                default -> log.warn("Unknown step for order-service: {}", command.step());
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
