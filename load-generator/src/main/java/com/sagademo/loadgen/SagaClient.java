package com.sagademo.loadgen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Drives a single saga through either tier's identical /sagas REST contract (see
 * orchestrator-custom.SagaController / orchestrator-temporal.SagaController): POST /sagas to
 * start, then poll GET /sagas/{id} until a terminal status is observed.
 */
public class SagaClient {

    // Only these steps accept a simulateFailure flag downstream (see OrderFulfillmentActivities /
    // SagaOrchestratorEngine) - COMPLETE_ORDER and the *_INVENTORY/PAYMENT/ORDER undo commands don't.
    private static final String[] INJECTABLE_STEPS = {
            "CREATE_ORDER", "RESERVE_INVENTORY", "CHARGE_PAYMENT", "CONFIRM_SHIPMENT"
    };

    private static final Set<String> TERMINAL_STATUSES = Set.of("COMPLETED", "COMPENSATED", "FAILED");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LoadGeneratorConfig config;

    public SagaClient(HttpClient httpClient, LoadGeneratorConfig config) {
        this.httpClient = httpClient;
        this.config = config;
    }

    public SagaRunResult run() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String failureStep = random.nextDouble() < config.failureRate()
                ? INJECTABLE_STEPS[random.nextInt(INJECTABLE_STEPS.length)]
                : null;

        long submittedAt = System.currentTimeMillis();
        try {
            String sagaId = start(random, failureStep);
            return poll(sagaId, failureStep, submittedAt);
        } catch (Exception e) {
            long now = System.currentTimeMillis();
            return new SagaRunResult(null, config.tier(), failureStep, submittedAt, now,
                    now - submittedAt, SagaOutcome.ERROR, e.getMessage());
        }
    }

    private String start(ThreadLocalRandom random, String failureStep) throws IOException, InterruptedException {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("customerId", UUID.randomUUID());
        body.put("amount", BigDecimal.valueOf(random.nextDouble(5.0, 500.0)).setScale(2, java.math.RoundingMode.HALF_UP));
        body.put("itemId", "SKU-" + random.nextInt(1, 20));
        body.put("quantity", random.nextInt(1, 5));
        body.put("carrier", random.nextBoolean() ? "UPS" : "FEDEX");
        body.put("address", "123 Load Test Ave");
        if (failureStep != null) {
            body.put("simulateFailureAtStep", failureStep);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.baseUrl() + "/sagas"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 202) {
            throw new IOException("POST /sagas returned " + response.statusCode() + ": " + response.body());
        }
        JsonNode json = objectMapper.readTree(response.body());
        return json.get("sagaId").asText();
    }

    private SagaRunResult poll(String sagaId, String failureStep, long submittedAt) throws IOException, InterruptedException {
        long deadline = submittedAt + config.pollTimeoutMs();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.baseUrl() + "/sagas/" + sagaId))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        while (System.currentTimeMillis() < deadline) {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String status = objectMapper.readTree(response.body()).get("status").asText();
                if (TERMINAL_STATUSES.contains(status)) {
                    long now = System.currentTimeMillis();
                    return new SagaRunResult(sagaId, config.tier(), failureStep, submittedAt, now,
                            now - submittedAt, SagaOutcome.valueOf(status), null);
                }
            }
            Thread.sleep(config.pollIntervalMs());
        }

        long now = System.currentTimeMillis();
        return new SagaRunResult(sagaId, config.tier(), failureStep, submittedAt, now,
                now - submittedAt, SagaOutcome.TIMEOUT, "Saga did not reach a terminal status within " + config.pollTimeoutMs() + "ms");
    }
}
