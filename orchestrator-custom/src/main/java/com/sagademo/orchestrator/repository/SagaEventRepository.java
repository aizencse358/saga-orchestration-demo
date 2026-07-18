package com.sagademo.orchestrator.repository;

import com.sagademo.orchestrator.model.SagaEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SagaEventRepository extends JpaRepository<SagaEvent, Long> {

    List<SagaEvent> findBySagaIdOrderByCreatedAtAsc(UUID sagaId);
}
