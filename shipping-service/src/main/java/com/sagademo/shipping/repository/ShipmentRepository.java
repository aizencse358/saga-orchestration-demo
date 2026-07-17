package com.sagademo.shipping.repository;

import com.sagademo.shipping.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    Optional<Shipment> findByIdempotencyKey(String idempotencyKey);
}
