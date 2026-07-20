package com.sagademo.loadgen;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * All arguments are optional flags of the form --key=value; unknown keys are rejected so a typo
 * fails fast instead of silently running with defaults.
 */
public record LoadGeneratorConfig(
        String tier,
        String baseUrl,
        int requests,
        int concurrency,
        double failureRate,
        long pollIntervalMs,
        long pollTimeoutMs,
        Path outputCsv
) {

    private static final Map<String, String> DEFAULT_BASE_URLS = Map.of(
            "custom", "http://localhost:8090",
            "temporal", "http://localhost:8091"
    );

    public static LoadGeneratorConfig parse(String[] args) {
        Map<String, String> flags = new HashMap<>();
        for (String arg : args) {
            if (!arg.startsWith("--") || !arg.contains("=")) {
                throw new IllegalArgumentException("Unrecognized argument: " + arg + " (expected --key=value)");
            }
            String[] kv = arg.substring(2).split("=", 2);
            flags.put(kv[0], kv[1]);
        }

        String tier = flags.getOrDefault("tier", "custom");
        if (!DEFAULT_BASE_URLS.containsKey(tier)) {
            throw new IllegalArgumentException("Unknown --tier '" + tier + "', expected one of " + DEFAULT_BASE_URLS.keySet());
        }

        String baseUrl = flags.getOrDefault("base-url", DEFAULT_BASE_URLS.get(tier));
        int requests = Integer.parseInt(flags.getOrDefault("requests", "100"));
        int concurrency = Integer.parseInt(flags.getOrDefault("concurrency", "10"));
        double failureRate = Double.parseDouble(flags.getOrDefault("failure-rate", "0.0"));
        long pollIntervalMs = Long.parseLong(flags.getOrDefault("poll-interval-ms", "200"));
        long pollTimeoutMs = Long.parseLong(flags.getOrDefault("poll-timeout-ms", "30000"));
        Path outputCsv = Path.of(flags.getOrDefault("output",
                "results/" + tier + "-" + System.currentTimeMillis() + ".csv"));

        if (failureRate < 0.0 || failureRate > 1.0) {
            throw new IllegalArgumentException("--failure-rate must be between 0.0 and 1.0, got " + failureRate);
        }

        return new LoadGeneratorConfig(tier, baseUrl, requests, concurrency, failureRate,
                pollIntervalMs, pollTimeoutMs, outputCsv);
    }
}
