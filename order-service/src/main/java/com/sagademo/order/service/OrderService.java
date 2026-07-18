package com.sagademo.order.service;

import com.sagademo.order.dto.CreateOrderRequest;
import com.sagademo.order.exception.SimulatedFailureException;
import com.sagademo.order.model.Order;
import com.sagademo.order.model.OrderStatus;
import com.sagademo.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order create(CreateOrderRequest request) {
        if (Boolean.TRUE.equals(request.simulateFailure())) {
            throw new SimulatedFailureException("Simulated failure creating order for saga " + request.sagaId());
        }
        return orderRepository.findByIdempotencyKey(request.idempotencyKey())
                .orElseGet(() -> {
                    Order order = new Order();
                    order.setSagaId(request.sagaId());
                    order.setCustomerId(request.customerId());
                    order.setAmount(request.amount());
                    order.setStatus(OrderStatus.PENDING);
                    order.setIdempotencyKey(request.idempotencyKey());
                    return orderRepository.save(order);
                });
    }

    public Order cancel(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return order;
        }
        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    public Order confirm(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            return order;
        }
        order.setStatus(OrderStatus.CONFIRMED);
        return orderRepository.save(order);
    }
}
