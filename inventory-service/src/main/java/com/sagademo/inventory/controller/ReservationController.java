package com.sagademo.inventory.controller;

import com.sagademo.inventory.dto.CreateReservationRequest;
import com.sagademo.inventory.model.Reservation;
import com.sagademo.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final InventoryService inventoryService;

    public ReservationController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<Reservation> create(@Valid @RequestBody CreateReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.reserve(request));
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<Reservation> release(@PathVariable UUID id) {
        return ResponseEntity.ok(inventoryService.release(id));
    }
}
