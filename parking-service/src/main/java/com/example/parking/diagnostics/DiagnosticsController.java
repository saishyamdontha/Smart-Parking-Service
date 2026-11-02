package com.example.parking.diagnostics;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Service-level diagnostics: database status, error codes and recent errors. */
@RestController
public class DiagnosticsController {

    private final DiagnosticsRegistry registry;
    private final DataSource dataSource;
    private final Instant started = Instant.now();

    public DiagnosticsController(DiagnosticsRegistry registry, DataSource dataSource) {
        this.registry = registry;
        this.dataSource = dataSource;
    }

    @GetMapping("/diagnostics")
    public Map<String, Object> diagnostics() {
        Map<String, Object> dependencies = new LinkedHashMap<>();
        dependencies.put("database", database());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("service", "parking-service");
        out.put("uptimeSeconds", Duration.between(started, Instant.now()).toSeconds());
        out.put("dependencies", dependencies);
        out.put("errorCounts", registry.counts());
        out.put("recentErrors", registry.recent());
        return out;
    }

    @PostMapping("/diagnostics/reset")
    public Map<String, String> reset() {
        registry.reset();
        return Map.of("status", "reset");
    }

    private String database() {
        try (Connection c = dataSource.getConnection()) {
            return c.isValid(1) ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN: " + e.getClass().getSimpleName();
        }
    }
}
