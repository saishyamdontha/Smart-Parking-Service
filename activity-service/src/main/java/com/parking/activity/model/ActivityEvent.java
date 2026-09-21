package com.parking.activity.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "activity_events")
public class ActivityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "space_id")
    private Long spaceId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    public ActivityEvent() {}

    public ActivityEvent(String eventType, Long sessionId, Long spaceId, String userId) {
        this.eventType = eventType;
        this.sessionId = sessionId;
        this.spaceId = spaceId;
        this.userId = userId;
        this.recordedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getEventType() { return eventType; }
    public Long getSessionId() { return sessionId; }
    public Long getSpaceId() { return spaceId; }
    public String getUserId() { return userId; }
    public Instant getRecordedAt() { return recordedAt; }
}
