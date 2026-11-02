package com.parking.session.client;

import com.parking.session.diagnostics.DiagnosticsRegistry;
import com.parking.session.diagnostics.Http;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * Records activity events asynchronously. Failures do not fail the session (by design),
 * but they are now counted in /diagnostics instead of disappearing into a log line.
 */
@Component
public class ActivityEventClient {

    private static final Logger log = LoggerFactory.getLogger(ActivityEventClient.class);
    private final RestClient restClient;
    private final DiagnosticsRegistry diagnostics;

    public ActivityEventClient(@Value("${activity.service.base-url}") String baseUrl,
                               @Value("${activity.service.timeout-ms:2000}") int timeoutMs,
                               DiagnosticsRegistry diagnostics) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(Http.factory(timeoutMs))
                .requestInterceptor(Http.correlation())
                .build();
        this.diagnostics = diagnostics;
    }

    @Async
    public void record(String eventType, Long sessionId, Long spaceId, String userId) {
        try {
            restClient.post()
                    .uri("/api/events")
                    .body(Map.of(
                            "eventType", eventType,
                            "sessionId", sessionId,
                            "spaceId", spaceId,
                            "userId", userId
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String code = "SES-ACTIVITY-" + (status >= 500 ? "HTTP_5XX" : "HTTP_4XX");
            diagnostics.record(code, "event " + eventType + " for session " + sessionId + " lost: HTTP " + status);
            log.warn("Failed to record activity event {} for session {}: HTTP {}", eventType, sessionId, status);
        } catch (ResourceAccessException e) {
            String code = "SES-ACTIVITY-" + (Http.isTimeout(e) ? "TIMEOUT" : "UNREACHABLE");
            diagnostics.record(code, "event " + eventType + " for session " + sessionId + " lost: " + e.getMessage());
            log.warn("Failed to record activity event {} for session {}: {}", eventType, sessionId, e.getMessage());
        }
    }
}
