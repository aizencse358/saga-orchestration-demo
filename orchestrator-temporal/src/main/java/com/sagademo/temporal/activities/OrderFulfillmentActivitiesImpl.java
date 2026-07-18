package com.sagademo.temporal.activities;

import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Calls the four downstream services over plain REST, reusing their existing idempotency-key
 * scheme (sagaId:STEP) so a Temporal activity retry - or a full workflow replay after a worker
 * crash - is naturally safe against the services' dedup-by-idempotencyKey logic.
 */
@Component
@ActivityImpl(taskQueues = "order-fulfillment-queue")
public class OrderFulfillmentActivitiesImpl implements OrderFulfillmentActivities {

    private final RestClient orderClient;
    private final RestClient inventoryClient;
    private final RestClient paymentClient;
    private final RestClient shippingClient;

    public OrderFulfillmentActivitiesImpl(
            @Value("${services.order.base-url}") String orderBaseUrl,
            @Value("${services.inventory.base-url}") String inventoryBaseUrl,
            @Value("${services.payment.base-url}") String paymentBaseUrl,
            @Value("${services.shipping.base-url}") String shippingBaseUrl) {
        this.orderClient = RestClient.create(orderBaseUrl);
        this.inventoryClient = RestClient.create(inventoryBaseUrl);
        this.paymentClient = RestClient.create(paymentBaseUrl);
        this.shippingClient = RestClient.create(shippingBaseUrl);
    }

    @Override
    public UUID createOrder(UUID sagaId, UUID customerId, BigDecimal amount, boolean simulateFailure) {
        return create(orderClient, "/orders", "CREATE_ORDER", sagaId, Map.of(
                "customerId", customerId,
                "amount", amount
        ), simulateFailure);
    }

    @Override
    public void cancelOrder(UUID sagaId, UUID orderId) {
        undo(orderClient, "/orders/" + orderId + "/cancel");
    }

    @Override
    public void completeOrder(UUID sagaId, UUID orderId) {
        undo(orderClient, "/orders/" + orderId + "/confirm");
    }

    @Override
    public UUID reserveInventory(UUID sagaId, String itemId, int quantity, boolean simulateFailure) {
        return create(inventoryClient, "/reservations", "RESERVE_INVENTORY", sagaId, Map.of(
                "itemId", itemId,
                "quantity", quantity
        ), simulateFailure);
    }

    @Override
    public void releaseInventory(UUID sagaId, UUID reservationId) {
        undo(inventoryClient, "/reservations/" + reservationId + "/release");
    }

    @Override
    public UUID chargePayment(UUID sagaId, UUID customerId, BigDecimal amount, boolean simulateFailure) {
        return create(paymentClient, "/payments", "CHARGE_PAYMENT", sagaId, Map.of(
                "customerId", customerId,
                "amount", amount
        ), simulateFailure);
    }

    @Override
    public void refundPayment(UUID sagaId, UUID paymentId) {
        undo(paymentClient, "/payments/" + paymentId + "/refund");
    }

    @Override
    public UUID confirmShipment(UUID sagaId, String carrier, String address, boolean simulateFailure) {
        return create(shippingClient, "/shipments", "CONFIRM_SHIPMENT", sagaId, Map.of(
                "carrier", carrier,
                "address", address
        ), simulateFailure);
    }

    private UUID create(RestClient client, String path, String step, UUID sagaId, Map<String, Object> fields, boolean simulateFailure) {
        Map<String, Object> body = new java.util.HashMap<>(fields);
        body.put("idempotencyKey", sagaId + ":" + step);
        body.put("sagaId", sagaId);
        body.put("simulateFailure", simulateFailure);

        try {
            Map<?, ?> response = client.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            return UUID.fromString(String.valueOf(response.get("id")));
        } catch (HttpClientErrorException.UnprocessableEntity e) {
            throw ApplicationFailure.newNonRetryableFailure(
                    "Simulated failure at step " + step, "SIMULATED_FAILURE");
        }
    }

    private void undo(RestClient client, String path) {
        client.post().uri(path).retrieve().toBodilessEntity();
    }
}
