package com.sagademo.orchestrator.engine;

import com.sagademo.orchestrator.dto.StartSagaRequest;
import com.sagademo.orchestrator.messaging.SagaCommand;
import com.sagademo.orchestrator.messaging.SagaCommandProducer;
import com.sagademo.orchestrator.messaging.SagaReply;
import com.sagademo.orchestrator.model.Saga;
import com.sagademo.orchestrator.model.SagaDirection;
import com.sagademo.orchestrator.model.SagaEvent;
import com.sagademo.orchestrator.model.SagaStatus;
import com.sagademo.orchestrator.repository.SagaEventRepository;
import com.sagademo.orchestrator.repository.SagaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.sagademo.orchestrator.engine.SagaDefinition.FORWARD_STEPS;

@Service
public class SagaOrchestratorEngine {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestratorEngine.class);

    private final SagaRepository sagaRepository;
    private final SagaEventRepository sagaEventRepository;
    private final SagaCommandProducer commandProducer;

    public SagaOrchestratorEngine(SagaRepository sagaRepository, SagaEventRepository sagaEventRepository,
                                   SagaCommandProducer commandProducer) {
        this.sagaRepository = sagaRepository;
        this.sagaEventRepository = sagaEventRepository;
        this.commandProducer = commandProducer;
    }

    @Transactional
    public Saga startSaga(StartSagaRequest request) {
        Saga saga = new Saga();
        saga.setId(UUID.randomUUID());
        saga.setStatus(SagaStatus.IN_PROGRESS);
        saga.setStepIndex(0);
        saga.setDirection(SagaDirection.FORWARD);
        saga.setCustomerId(request.customerId());
        saga.setAmount(request.amount());
        saga.setItemId(request.itemId());
        saga.setQuantity(request.quantity());
        saga.setCarrier(request.carrier());
        saga.setAddress(request.address());
        saga.setSimulateFailureAtStep(request.simulateFailureAtStep());
        saga = sagaRepository.save(saga);

        logEvent(saga.getId(), FORWARD_STEPS.get(0).step(), "STARTED", null);
        sendForwardCommand(saga, 0);
        return saga;
    }

    @Transactional
    public void handleReply(SagaReply reply) {
        Saga saga = sagaRepository.findById(reply.sagaId()).orElse(null);
        if (saga == null) {
            log.warn("Received reply for unknown sagaId={}", reply.sagaId());
            return;
        }

        String expectedStep = saga.getDirection() == SagaDirection.FORWARD
                ? FORWARD_STEPS.get(saga.getStepIndex()).step()
                : FORWARD_STEPS.get(saga.getStepIndex()).compensationStep();

        if (!reply.step().equals(expectedStep)) {
            log.warn("Ignoring stale/duplicate reply sagaId={} step={} expected={}", saga.getId(), reply.step(), expectedStep);
            return;
        }

        if ("SUCCESS".equals(reply.status())) {
            handleSuccess(saga, reply);
        } else {
            handleFailure(saga, reply);
        }
    }

    private void handleSuccess(Saga saga, SagaReply reply) {
        logEvent(saga.getId(), reply.step(), "SUCCEEDED", null);

        if (saga.getDirection() == SagaDirection.FORWARD) {
            if (!"COMPLETE_ORDER".equals(reply.step())) {
                setResourceIdForIndex(saga, saga.getStepIndex(), reply.resourceId());
            }
            if (saga.getStepIndex() == FORWARD_STEPS.size() - 1) {
                saga.setStatus(SagaStatus.COMPLETED);
                sagaRepository.save(saga);
                return;
            }
            saga.setStepIndex(saga.getStepIndex() + 1);
            sagaRepository.save(saga);
            sendForwardCommand(saga, saga.getStepIndex());
        } else {
            int next = previousCompensableIndex(saga.getStepIndex() - 1);
            if (next < 0) {
                saga.setStatus(SagaStatus.COMPENSATED);
                sagaRepository.save(saga);
                return;
            }
            saga.setStepIndex(next);
            sagaRepository.save(saga);
            sendCompensationCommand(saga, next);
        }
    }

    private void handleFailure(Saga saga, SagaReply reply) {
        logEvent(saga.getId(), reply.step(), "FAILED", reply.errorMessage());

        if (saga.getDirection() == SagaDirection.COMPENSATE) {
            log.error("Compensation step failed sagaId={} step={} detail={} - saga is now FAILED and needs manual attention",
                    saga.getId(), reply.step(), reply.errorMessage());
            saga.setStatus(SagaStatus.FAILED);
            sagaRepository.save(saga);
            return;
        }

        int start = previousCompensableIndex(saga.getStepIndex() - 1);
        saga.setDirection(SagaDirection.COMPENSATE);
        if (start < 0) {
            saga.setStatus(SagaStatus.COMPENSATED);
            sagaRepository.save(saga);
            return;
        }
        saga.setStatus(SagaStatus.COMPENSATING);
        saga.setStepIndex(start);
        sagaRepository.save(saga);
        sendCompensationCommand(saga, start);
    }

    private int previousCompensableIndex(int fromIndex) {
        int index = fromIndex;
        while (index >= 0 && FORWARD_STEPS.get(index).compensationStep() == null) {
            index--;
        }
        return index;
    }

    void sendForwardCommand(Saga saga, int index) {
        StepDefinition def = FORWARD_STEPS.get(index);
        String step = def.step();
        SagaCommand command = new SagaCommand(
                saga.getId(),
                step,
                def.targetService(),
                saga.getId() + ":" + step,
                "COMPLETE_ORDER".equals(step) ? saga.getOrderId() : null,
                ("CREATE_ORDER".equals(step) || "CHARGE_PAYMENT".equals(step)) ? saga.getCustomerId() : null,
                ("CREATE_ORDER".equals(step) || "CHARGE_PAYMENT".equals(step)) ? saga.getAmount() : null,
                "RESERVE_INVENTORY".equals(step) ? saga.getItemId() : null,
                "RESERVE_INVENTORY".equals(step) ? saga.getQuantity() : null,
                "CONFIRM_SHIPMENT".equals(step) ? saga.getCarrier() : null,
                "CONFIRM_SHIPMENT".equals(step) ? saga.getAddress() : null,
                step.equals(saga.getSimulateFailureAtStep())
        );
        commandProducer.send(command);
    }

    void sendCompensationCommand(Saga saga, int index) {
        StepDefinition def = FORWARD_STEPS.get(index);
        String compStep = def.compensationStep();
        SagaCommand command = new SagaCommand(
                saga.getId(),
                compStep,
                def.targetService(),
                saga.getId() + ":" + compStep,
                getResourceIdForIndex(saga, index),
                null, null, null, null, null, null,
                false
        );
        commandProducer.send(command);
    }

    private UUID getResourceIdForIndex(Saga saga, int index) {
        return switch (index) {
            case 0 -> saga.getOrderId();
            case 1 -> saga.getReservationId();
            case 2 -> saga.getPaymentId();
            case 3 -> saga.getShipmentId();
            default -> null;
        };
    }

    private void setResourceIdForIndex(Saga saga, int index, UUID resourceId) {
        switch (index) {
            case 0 -> saga.setOrderId(resourceId);
            case 1 -> saga.setReservationId(resourceId);
            case 2 -> saga.setPaymentId(resourceId);
            case 3 -> saga.setShipmentId(resourceId);
            default -> { }
        }
    }

    private void logEvent(UUID sagaId, String step, String status, String detail) {
        SagaEvent event = new SagaEvent();
        event.setSagaId(sagaId);
        event.setStep(step);
        event.setStatus(status);
        event.setDetail(detail);
        sagaEventRepository.save(event);
    }
}
