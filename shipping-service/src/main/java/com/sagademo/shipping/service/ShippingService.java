package com.sagademo.shipping.service;

import com.sagademo.shipping.dto.CreateShipmentRequest;
import com.sagademo.shipping.exception.SimulatedFailureException;
import com.sagademo.shipping.model.Shipment;
import com.sagademo.shipping.model.ShipmentStatus;
import com.sagademo.shipping.repository.ShipmentRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ShippingService {

    private final ShipmentRepository shipmentRepository;

    public ShippingService(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }

    public Shipment confirm(CreateShipmentRequest request) {
        if (Boolean.TRUE.equals(request.simulateFailure())) {
            throw new SimulatedFailureException("Simulated failure confirming shipment for saga " + request.sagaId());
        }
        return shipmentRepository.findByIdempotencyKey(request.idempotencyKey())
                .orElseGet(() -> {
                    Shipment shipment = new Shipment();
                    shipment.setSagaId(request.sagaId());
                    shipment.setCarrier(request.carrier());
                    shipment.setAddress(request.address());
                    shipment.setStatus(ShipmentStatus.CONFIRMED);
                    shipment.setIdempotencyKey(request.idempotencyKey());
                    return shipmentRepository.save(shipment);
                });
    }

    public Shipment cancel(UUID shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new NoSuchElementException("Shipment not found: " + shipmentId));
        if (shipment.getStatus() == ShipmentStatus.CANCELLED) {
            return shipment;
        }
        shipment.setStatus(ShipmentStatus.CANCELLED);
        return shipmentRepository.save(shipment);
    }
}
