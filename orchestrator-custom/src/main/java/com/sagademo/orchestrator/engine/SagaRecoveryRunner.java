package com.sagademo.orchestrator.engine;

import com.sagademo.orchestrator.model.Saga;
import com.sagademo.orchestrator.model.SagaDirection;
import com.sagademo.orchestrator.model.SagaStatus;
import com.sagademo.orchestrator.repository.SagaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SagaRecoveryRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SagaRecoveryRunner.class);

    private final SagaRepository sagaRepository;
    private final SagaOrchestratorEngine engine;

    public SagaRecoveryRunner(SagaRepository sagaRepository, SagaOrchestratorEngine engine) {
        this.sagaRepository = sagaRepository;
        this.engine = engine;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Saga> inFlight = sagaRepository.findByStatusIn(List.of(SagaStatus.IN_PROGRESS, SagaStatus.COMPENSATING));
        if (inFlight.isEmpty()) {
            return;
        }
        log.info("Recovering {} in-flight saga(s) after restart", inFlight.size());
        for (Saga saga : inFlight) {
            if (saga.getDirection() == SagaDirection.FORWARD) {
                log.info("Resuming sagaId={} at step_index={} (forward)", saga.getId(), saga.getStepIndex());
                engine.sendForwardCommand(saga, saga.getStepIndex());
            } else {
                log.info("Resuming sagaId={} at step_index={} (compensating)", saga.getId(), saga.getStepIndex());
                engine.sendCompensationCommand(saga, saga.getStepIndex());
            }
        }
    }
}
