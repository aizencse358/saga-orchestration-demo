package com.sagademo.payment.controller;

import com.sagademo.payment.dto.ChargePaymentRequest;
import com.sagademo.payment.model.Payment;
import com.sagademo.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Payment> create(@Valid @RequestBody ChargePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.charge(request));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<Payment> refund(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.refund(id));
    }
}
