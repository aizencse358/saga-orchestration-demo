package com.sagademo.shipping.controller;

import com.sagademo.shipping.dto.CreateShipmentRequest;
import com.sagademo.shipping.model.Shipment;
import com.sagademo.shipping.service.ShippingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/shipments")
public class ShipmentController {

    private final ShippingService shippingService;

    public ShipmentController(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    @PostMapping
    public ResponseEntity<Shipment> create(@Valid @RequestBody CreateShipmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shippingService.confirm(request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Shipment> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(shippingService.cancel(id));
    }
}
