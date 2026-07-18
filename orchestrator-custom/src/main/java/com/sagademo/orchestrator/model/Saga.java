package com.sagademo.orchestrator.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sagas")
@Getter
@Setter
@NoArgsConstructor
public class Saga {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStatus status;

    @Column(name = "step_index", nullable = false)
    private int stepIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaDirection direction;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "item_id", nullable = false)
    private String itemId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private String carrier;

    @Column(nullable = false)
    private String address;

    @Column(name = "simulate_failure_at_step")
    private String simulateFailureAtStep;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "reservation_id")
    private UUID reservationId;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "shipment_id")
    private UUID shipmentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
