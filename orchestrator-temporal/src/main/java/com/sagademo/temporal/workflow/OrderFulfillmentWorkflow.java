package com.sagademo.temporal.workflow;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrderFulfillmentWorkflow {

    @WorkflowMethod
    SagaResult execute(StartSagaInput input);

    @QueryMethod
    SagaStatusView getStatus();
}
