package com.parking.session.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sessions")
public class ParkingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    public ParkingSession() {}

    public ParkingSession(String userId, Long spaceId) {
        this.userId = userId;
        this.spaceId = spaceId;
        this.status = SessionStatus.ACTIVE;
        this.startedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public Long getSpaceId() { return spaceId; }
    public SessionStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }

    public void end() {
        this.status = SessionStatus.ENDED;
        this.endedAt = Instant.now();
    }
}
