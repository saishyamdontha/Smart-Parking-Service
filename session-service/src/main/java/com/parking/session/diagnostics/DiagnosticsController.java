package com.parking.session.diagnostics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Service-level diagnostics: error codes, recent errors and the status of each dependency. */
@RestController
public class DiagnosticsController {

    private final DiagnosticsRegistry registry;
    private final DataSource dataSource;
    private final String parkingUrl;
    private final String activityUrl;
    private final RestClient probe = RestClient.builder().requestFactory(Http.factory(1000)).build();
    private final Instant started = Instant.now();

    public DiagnosticsController(DiagnosticsRegistry registry, DataSource dataSource,
                                 @Value("${parking.service.base-url}") String parkingUrl,
                                 @Value("${activity.service.base-url}") String activityUrl) {
        this.registry = registry;
        this.dataSource = dataSource;
        this.parkingUrl = parkingUrl;
        this.activityUrl = activityUrl;
    }

    @GetMapping("/diagnostics")
    public Map<String, Object> diagnostics() {
        Map<String, Object> dependencies = new LinkedHashMap<>();
        dependencies.put("database", database());
        dependencies.put("parking-service", probe(parkingUrl));
        dependencies.put("activity-service", probe(activityUrl));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("service", "session-service");
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

    private String probe(String baseUrl) {
        try {
            String body = probe.get().uri(baseUrl + "/actuator/health").retrieve().body(String.class);
            return body != null && body.contains("\"UP\"") ? "UP" : "DOWN";
        } catch (RestClientResponseException e) {
            return "DOWN (HTTP " + e.getStatusCode().value() + ")";
        } catch (Exception e) {
            return "UNREACHABLE: " + e.getClass().getSimpleName();
        }
    }
}
