package com.parking.session.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class ActivityEventClient {

    private static final Logger log = LoggerFactory.getLogger(ActivityEventClient.class);
    private final RestClient restClient;

    public ActivityEventClient(@Value("${activity.service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
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
        } catch (Exception e) {
            log.warn("Failed to record activity event {} for session {}: {}", eventType, sessionId, e.getMessage());
        }
    }
}
