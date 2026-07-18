package com.sagademo.order.controller;

import com.sagademo.order.dto.CreateOrderRequest;
import com.sagademo.order.model.Order;
import com.sagademo.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> create(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Order> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.cancel(id));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<Order> confirm(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.confirm(id));
    }
}
