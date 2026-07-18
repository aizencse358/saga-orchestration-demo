package com.sagademo.temporal.workflow;

import com.sagademo.temporal.activities.OrderFulfillmentActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Same fixed 5-step sequence as Tier 1's orchestrator-custom, but with no hand-rolled event log,
 * state machine, or crash-recovery runner: Temporal's durable workflow execution IS the recovery
 * mechanism, and io.temporal.workflow.Saga IS the compensation mechanism.
 */
@Component
@WorkflowImpl(taskQueues = "order-fulfillment-queue")
public class OrderFulfillmentWorkflowImpl implements OrderFulfillmentWorkflow {

    private static final String STEP_CREATE_ORDER = "CREATE_ORDER";
    private static final String STEP_RESERVE_INVENTORY = "RESERVE_INVENTORY";
    private static final String STEP_CHARGE_PAYMENT = "CHARGE_PAYMENT";
    private static final String STEP_CONFIRM_SHIPMENT = "CONFIRM_SHIPMENT";
    private static final String STEP_COMPLETE_ORDER = "COMPLETE_ORDER";

    private String currentStep = STEP_CREATE_ORDER;
    private String status = "IN_PROGRESS";
    private UUID orderId;
    private UUID reservationId;
    private UUID paymentId;
    private UUID shipmentId;

    @Override
    public SagaResult execute(StartSagaInput input) {
        UUID sagaId = input.sagaId();
        Saga saga = new Saga(new Saga.Options.Builder().setParallelCompensation(false).build());
        OrderFulfillmentActivities activities = Workflow.newActivityStub(
                OrderFulfillmentActivities.class,
                ActivityOptions.newBuilder()
                        .setStartToCloseTimeout(Duration.ofSeconds(10))
                        .setRetryOptions(RetryOptions.newBuilder()
                                .setMaximumAttempts(3)
                                .setInitialInterval(Duration.ofSeconds(1))
                                .build())
                        .build());

        try {
            currentStep = STEP_CREATE_ORDER;
            orderId = activities.createOrder(sagaId, input.customerId(), input.amount(),
                    STEP_CREATE_ORDER.equals(input.simulateFailureAtStep()));
            saga.addCompensation(() -> activities.cancelOrder(sagaId, orderId));

            currentStep = STEP_RESERVE_INVENTORY;
            reservationId = activities.reserveInventory(sagaId, input.itemId(), input.quantity(),
                    STEP_RESERVE_INVENTORY.equals(input.simulateFailureAtStep()));
            saga.addCompensation(() -> activities.releaseInventory(sagaId, reservationId));

            currentStep = STEP_CHARGE_PAYMENT;
            paymentId = activities.chargePayment(sagaId, input.customerId(), input.amount(),
                    STEP_CHARGE_PAYMENT.equals(input.simulateFailureAtStep()));
            saga.addCompensation(() -> activities.refundPayment(sagaId, paymentId));

            currentStep = STEP_CONFIRM_SHIPMENT;
            shipmentId = activities.confirmShipment(sagaId, input.carrier(), input.address(),
                    STEP_CONFIRM_SHIPMENT.equals(input.simulateFailureAtStep()));
            // no compensation for shipment: nothing runs after it except completing the order

            currentStep = STEP_COMPLETE_ORDER;
            activities.completeOrder(sagaId, orderId);

            status = "COMPLETED";
            return new SagaResult(sagaId, status, orderId, reservationId, paymentId, shipmentId);
        } catch (Exception e) {
            status = "COMPENSATING";
            try {
                saga.compensate();
                status = "COMPENSATED";
            } catch (Exception compensationFailure) {
                status = "FAILED";
            }
            return new SagaResult(sagaId, status, orderId, reservationId, paymentId, shipmentId);
        }
    }

    @Override
    public SagaStatusView getStatus() {
        return new SagaStatusView(currentStep, status, orderId, reservationId, paymentId, shipmentId);
    }
}
