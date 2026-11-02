package com.parking.activity.diagnostics;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/** In-memory record of diagnostic error codes: counts per code and the most recent errors. */
@Component
public class DiagnosticsRegistry {

    public record ErrorEntry(Instant at, String code, String message, String correlationId) {}

    private static final int MAX_RECENT = 50;
    private final Map<String, AtomicLong> counts = new ConcurrentHashMap<>();
    private final Deque<ErrorEntry> recent = new ConcurrentLinkedDeque<>();

    public void record(String code, String message) {
        counts.computeIfAbsent(code, k -> new AtomicLong()).incrementAndGet();
        recent.addFirst(new ErrorEntry(Instant.now(), code, message, MDC.get(CorrelationIdFilter.MDC_KEY)));
        while (recent.size() > MAX_RECENT) {
            recent.pollLast();
        }
    }

    public Map<String, Long> counts() {
        Map<String, Long> out = new TreeMap<>();
        counts.forEach((k, v) -> out.put(k, v.get()));
        return out;
    }

    public List<ErrorEntry> recent() {
        return new ArrayList<>(recent);
    }

    public void reset() {
        counts.clear();
        recent.clear();
    }
}
