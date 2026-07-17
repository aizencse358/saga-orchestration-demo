package com.sagademo.payment.service;

import com.sagademo.payment.dto.ChargePaymentRequest;
import com.sagademo.payment.model.Payment;
import com.sagademo.payment.model.PaymentStatus;
import com.sagademo.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment charge(ChargePaymentRequest request) {
        return paymentRepository.findByIdempotencyKey(request.idempotencyKey())
                .orElseGet(() -> {
                    Payment payment = new Payment();
                    payment.setSagaId(request.sagaId());
                    payment.setCustomerId(request.customerId());
                    payment.setAmount(request.amount());
                    payment.setStatus(PaymentStatus.CHARGED);
                    payment.setIdempotencyKey(request.idempotencyKey());
                    return paymentRepository.save(payment);
                });
    }

    public Payment refund(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoSuchElementException("Payment not found: " + paymentId));
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return payment;
        }
        payment.setStatus(PaymentStatus.REFUNDED);
        return paymentRepository.save(payment);
    }
}
