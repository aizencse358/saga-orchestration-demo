package com.sagademo.loadgen;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Fires `requests` sagas at one tier, `concurrency` in flight at a time, using a virtual thread
 * per saga (submit + poll-to-terminal). Produces a CSV under results/ plus a console summary.
 *
 * Usage: java -jar load-generator.jar --tier=custom --requests=200 --concurrency=20 --failure-rate=0.2
 */
public class LoadGeneratorMain {

    public static void main(String[] args) throws Exception {
        LoadGeneratorConfig config = LoadGeneratorConfig.parse(args);
        System.out.printf("Running %d sagas against tier=%s (%s), concurrency=%d, failure-rate=%.2f%n",
                config.requests(), config.tier(), config.baseUrl(), config.concurrency(), config.failureRate());

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();
        SagaClient sagaClient = new SagaClient(httpClient, config);

        ConcurrentLinkedQueue<SagaRunResult> results = new ConcurrentLinkedQueue<>();
        Semaphore inFlight = new Semaphore(config.concurrency());
        AtomicInteger completed = new AtomicInteger();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < config.requests(); i++) {
                inFlight.acquire();
                executor.submit(() -> {
                    try {
                        results.add(sagaClient.run());
                        int done = completed.incrementAndGet();
                        if (done % Math.max(1, config.requests() / 10) == 0) {
                            System.out.printf("  %d/%d sagas complete%n", done, config.requests());
                        }
                    } finally {
                        inFlight.release();
                    }
                });
            }
        } // executor.close() blocks until all submitted tasks finish

        List<SagaRunResult> resultList = new ArrayList<>(results);
        CsvResultsWriter.write(config.outputCsv(), resultList);
        printSummary(config, resultList);
        System.out.println("Results written to " + config.outputCsv().toAbsolutePath());
    }

    private static void printSummary(LoadGeneratorConfig config, List<SagaRunResult> results) {
        Map<SagaOutcome, Long> byOutcome = results.stream()
                .collect(Collectors.groupingBy(SagaRunResult::outcome, Collectors.counting()));

        System.out.println();
        System.out.println("=== Summary: tier=" + config.tier() + " ===");
        System.out.println("Total: " + results.size());
        for (SagaOutcome outcome : SagaOutcome.values()) {
            System.out.printf("  %-11s %d%n", outcome, byOutcome.getOrDefault(outcome, 0L));
        }

        List<Long> latencies = results.stream()
                .map(SagaRunResult::latencyMs)
                .sorted(Comparator.naturalOrder())
                .toList();
        if (!latencies.isEmpty()) {
            System.out.printf("Latency ms: p50=%d p95=%d p99=%d max=%d%n",
                    percentile(latencies, 0.50), percentile(latencies, 0.95),
                    percentile(latencies, 0.99), latencies.get(latencies.size() - 1));
        }
    }

    private static long percentile(List<Long> sortedLatencies, double p) {
        int index = (int) Math.ceil(p * sortedLatencies.size()) - 1;
        return sortedLatencies.get(Math.max(0, Math.min(index, sortedLatencies.size() - 1)));
    }
}
