package com.sagademo.orchestrator.controller;

import com.sagademo.orchestrator.dto.SagaEventResponse;
import com.sagademo.orchestrator.dto.SagaResponse;
import com.sagademo.orchestrator.dto.StartSagaRequest;
import com.sagademo.orchestrator.engine.SagaOrchestratorEngine;
import com.sagademo.orchestrator.model.Saga;
import com.sagademo.orchestrator.repository.SagaEventRepository;
import com.sagademo.orchestrator.repository.SagaRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/sagas")
public class SagaController {

    private final SagaOrchestratorEngine engine;
    private final SagaRepository sagaRepository;
    private final SagaEventRepository sagaEventRepository;

    public SagaController(SagaOrchestratorEngine engine, SagaRepository sagaRepository, SagaEventRepository sagaEventRepository) {
        this.engine = engine;
        this.sagaRepository = sagaRepository;
        this.sagaEventRepository = sagaEventRepository;
    }

    @PostMapping
    public ResponseEntity<SagaResponse> start(@Valid @RequestBody StartSagaRequest request) {
        Saga saga = engine.startSaga(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(SagaResponse.from(saga));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SagaResponse> get(@PathVariable UUID id) {
        Saga saga = sagaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Saga not found: " + id));
        return ResponseEntity.ok(SagaResponse.from(saga));
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<List<SagaEventResponse>> events(@PathVariable UUID id) {
        List<SagaEventResponse> events = sagaEventRepository.findBySagaIdOrderByCreatedAtAsc(id).stream()
                .map(SagaEventResponse::from)
                .toList();
        return ResponseEntity.ok(events);
    }
}
