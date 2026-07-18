package com.sagademo.temporal.activities;

import io.temporal.activity.ActivityInterface;

import java.math.BigDecimal;
import java.util.UUID;

@ActivityInterface
public interface OrderFulfillmentActivities {

    UUID createOrder(UUID sagaId, UUID customerId, BigDecimal amount, boolean simulateFailure);

    void cancelOrder(UUID sagaId, UUID orderId);

    void completeOrder(UUID sagaId, UUID orderId);

    UUID reserveInventory(UUID sagaId, String itemId, int quantity, boolean simulateFailure);

    void releaseInventory(UUID sagaId, UUID reservationId);

    UUID chargePayment(UUID sagaId, UUID customerId, BigDecimal amount, boolean simulateFailure);

    void refundPayment(UUID sagaId, UUID paymentId);

    UUID confirmShipment(UUID sagaId, String carrier, String address, boolean simulateFailure);
}
