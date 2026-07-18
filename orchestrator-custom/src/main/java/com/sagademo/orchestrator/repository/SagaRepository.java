package com.sagademo.orchestrator.repository;

import com.sagademo.orchestrator.model.Saga;
import com.sagademo.orchestrator.model.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SagaRepository extends JpaRepository<Saga, UUID> {

    List<Saga> findByStatusIn(List<SagaStatus> statuses);
}
