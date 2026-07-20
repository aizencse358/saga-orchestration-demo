package com.sagademo.loadgen;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CsvResultsWriter {

    public static void write(Path path, List<SagaRunResult> results) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("sagaId,tier,injectedFailureStep,submittedAtEpochMs,completedAtEpochMs,latencyMs,outcome,errorMessage\n");
            for (SagaRunResult r : results) {
                writer.write(String.join(",",
                        nullToEmpty(r.sagaId()),
                        r.tier(),
                        nullToEmpty(r.injectedFailureStep()),
                        String.valueOf(r.submittedAtEpochMs()),
                        String.valueOf(r.completedAtEpochMs()),
                        String.valueOf(r.latencyMs()),
                        r.outcome().name(),
                        escape(r.errorMessage())));
                writer.write("\n");
            }
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        String sanitized = s.replace("\"", "'").replace("\n", " ").replace(",", ";");
        return "\"" + sanitized + "\"";
    }
}
