package com.sagademo.payment.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sagademo.payment.dto.ChargePaymentRequest;
import com.sagademo.payment.model.Payment;
import com.sagademo.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class SagaCommandListener {

    private static final Logger log = LoggerFactory.getLogger(SagaCommandListener.class);
    private static final String TARGET_SERVICE = "PAYMENT";

    private final PaymentService paymentService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public SagaCommandListener(PaymentService paymentService, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "saga.commands", groupId = "payment-service")
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
                case "CHARGE_PAYMENT" -> {
                    Payment payment = paymentService.charge(new ChargePaymentRequest(
                            command.idempotencyKey(), command.sagaId(), command.customerId(),
                            command.amount(), command.simulateFailure()));
                    reply(command, "SUCCESS", payment.getId(), null);
                }
                case "REFUND_PAYMENT" -> {
                    Payment payment = paymentService.refund(command.resourceId());
                    reply(command, "SUCCESS", payment.getId(), null);
                }
                default -> log.warn("Unknown step for payment-service: {}", command.step());
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
