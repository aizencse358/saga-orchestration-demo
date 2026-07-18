package com.sagademo.temporal.controller;

import com.sagademo.temporal.dto.SagaEventResponse;
import com.sagademo.temporal.dto.SagaResponse;
import com.sagademo.temporal.dto.StartSagaRequest;
import com.sagademo.temporal.workflow.OrderFulfillmentWorkflow;
import com.sagademo.temporal.workflow.SagaResult;
import com.sagademo.temporal.workflow.SagaStatusView;
import com.sagademo.temporal.workflow.StartSagaInput;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.api.workflowservice.v1.GetWorkflowExecutionHistoryRequest;
import io.temporal.api.workflowservice.v1.GetWorkflowExecutionHistoryResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/sagas")
public class SagaController {

    private static final String TASK_QUEUE = "order-fulfillment-queue";

    private final WorkflowClient workflowClient;
    private final WorkflowServiceStubs serviceStubs;

    public SagaController(WorkflowClient workflowClient, WorkflowServiceStubs serviceStubs) {
        this.workflowClient = workflowClient;
        this.serviceStubs = serviceStubs;
    }

    @PostMapping
    public ResponseEntity<SagaResponse> start(@Valid @RequestBody StartSagaRequest request) {
        UUID sagaId = UUID.randomUUID();
        OrderFulfillmentWorkflow workflow = workflowClient.newWorkflowStub(
                OrderFulfillmentWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(sagaId.toString())
                        .setTaskQueue(TASK_QUEUE)
                        .build());

        StartSagaInput input = new StartSagaInput(
                sagaId,
                request.customerId(),
                request.amount(),
                request.itemId(),
                request.quantity(),
                request.carrier(),
                request.address(),
                request.simulateFailureAtStep());

        WorkflowClient.start(workflow::execute, input);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new SagaResponse(sagaId, "IN_PROGRESS", "CREATE_ORDER", null, null, null, null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SagaResponse> get(@PathVariable UUID id) {
        DescribeWorkflowExecutionResponse describe = serviceStubs.blockingStub().describeWorkflowExecution(
                DescribeWorkflowExecutionRequest.newBuilder()
                        .setNamespace(workflowClient.getOptions().getNamespace())
                        .setExecution(WorkflowExecution.newBuilder().setWorkflowId(id.toString()).build())
                        .build());
        WorkflowExecutionStatus execStatus = describe.getWorkflowExecutionInfo().getStatus();

        if (execStatus == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING) {
            OrderFulfillmentWorkflow workflow = workflowClient.newWorkflowStub(OrderFulfillmentWorkflow.class, id.toString());
            SagaStatusView view = workflow.getStatus();
            return ResponseEntity.ok(new SagaResponse(id, view.status(), view.currentStep(),
                    view.orderId(), view.reservationId(), view.paymentId(), view.shipmentId()));
        }

        if (execStatus == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED) {
            WorkflowStub untyped = workflowClient.newUntypedWorkflowStub(id.toString());
            SagaResult result = untyped.getResult(SagaResult.class);
            return ResponseEntity.ok(new SagaResponse(id, result.status(), null,
                    result.orderId(), result.reservationId(), result.paymentId(), result.shipmentId()));
        }

        return ResponseEntity.ok(new SagaResponse(id, execStatus.name(), null, null, null, null, null));
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<List<SagaEventResponse>> events(@PathVariable UUID id) {
        GetWorkflowExecutionHistoryResponse resp = serviceStubs.blockingStub().getWorkflowExecutionHistory(
                GetWorkflowExecutionHistoryRequest.newBuilder()
                        .setNamespace(workflowClient.getOptions().getNamespace())
                        .setExecution(WorkflowExecution.newBuilder().setWorkflowId(id.toString()).build())
                        .build());

        List<SagaEventResponse> events = new ArrayList<>();
        for (HistoryEvent e : resp.getHistory().getEventsList()) {
            events.add(new SagaEventResponse(
                    e.getEventId(),
                    e.getEventType().name(),
                    Instant.ofEpochSecond(e.getEventTime().getSeconds(), e.getEventTime().getNanos())));
        }
        return ResponseEntity.ok(events);
    }
}
