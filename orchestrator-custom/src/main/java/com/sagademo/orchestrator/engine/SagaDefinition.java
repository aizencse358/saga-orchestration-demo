package com.sagademo.orchestrator.engine;

import java.util.List;

public final class SagaDefinition {

    public static final List<StepDefinition> FORWARD_STEPS = List.of(
            new StepDefinition("CREATE_ORDER", "ORDER", "CANCEL_ORDER"),
            new StepDefinition("RESERVE_INVENTORY", "INVENTORY", "RELEASE_INVENTORY"),
            new StepDefinition("CHARGE_PAYMENT", "PAYMENT", "REFUND_PAYMENT"),
            new StepDefinition("CONFIRM_SHIPMENT", "SHIPPING", null),
            new StepDefinition("COMPLETE_ORDER", "ORDER", null)
    );

    private SagaDefinition() {
    }
}
