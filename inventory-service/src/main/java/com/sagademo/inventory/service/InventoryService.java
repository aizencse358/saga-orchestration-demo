package com.sagademo.inventory.service;

import com.sagademo.inventory.dto.CreateReservationRequest;
import com.sagademo.inventory.exception.SimulatedFailureException;
import com.sagademo.inventory.model.Reservation;
import com.sagademo.inventory.model.ReservationStatus;
import com.sagademo.inventory.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class InventoryService {

    private final ReservationRepository reservationRepository;

    public InventoryService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    public Reservation reserve(CreateReservationRequest request) {
        if (Boolean.TRUE.equals(request.simulateFailure())) {
            throw new SimulatedFailureException("Simulated failure reserving inventory for saga " + request.sagaId());
        }
        return reservationRepository.findByIdempotencyKey(request.idempotencyKey())
                .orElseGet(() -> {
                    Reservation reservation = new Reservation();
                    reservation.setSagaId(request.sagaId());
                    reservation.setItemId(request.itemId());
                    reservation.setQuantity(request.quantity());
                    reservation.setStatus(ReservationStatus.RESERVED);
                    reservation.setIdempotencyKey(request.idempotencyKey());
                    return reservationRepository.save(reservation);
                });
    }

    public Reservation release(UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found: " + reservationId));
        if (reservation.getStatus() == ReservationStatus.RELEASED) {
            return reservation;
        }
        reservation.setStatus(ReservationStatus.RELEASED);
        return reservationRepository.save(reservation);
    }
}
