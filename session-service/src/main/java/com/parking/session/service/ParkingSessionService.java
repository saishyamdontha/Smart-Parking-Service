package com.parking.session.service;

import com.parking.session.client.ActivityEventClient;
import com.parking.session.client.ParkingSpaceClient;
import com.parking.session.exception.ConflictException;
import com.parking.session.exception.NotFoundException;
import com.parking.session.model.ParkingSession;
import com.parking.session.model.SessionStatus;
import com.parking.session.repository.ParkingSessionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParkingSessionService {

    private final ParkingSessionRepository repository;
    private final ParkingSpaceClient parkingSpaceClient;
    private final ActivityEventClient activityEventClient;

    public ParkingSessionService(ParkingSessionRepository repository,
                                  ParkingSpaceClient parkingSpaceClient,
                                  ActivityEventClient activityEventClient) {
        this.repository = repository;
        this.parkingSpaceClient = parkingSpaceClient;
        this.activityEventClient = activityEventClient;
    }

    public List<ParkingSession> findAll() {
        return repository.findAll();
    }

    public ParkingSession findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Session " + id + " not found"));
    }

    public ParkingSession start(String userId, Long spaceId) {
        // synchronous call — if parking-service says the space isn't AVAILABLE,
        // this throws and no session row is ever created (no partial state).
        parkingSpaceClient.occupy(spaceId);

        ParkingSession session = new ParkingSession(userId, spaceId);
        session = repository.save(session);

        activityEventClient.record("SESSION_STARTED", session.getId(), spaceId, userId);
        return session;
    }

    public ParkingSession end(Long sessionId) {
        ParkingSession session = findById(sessionId);
        if (session.getStatus() == SessionStatus.ENDED) {
            throw new ConflictException("Session " + sessionId + " is already ended");
        }

        parkingSpaceClient.release(session.getSpaceId());

        session.end();
        session = repository.save(session);

        activityEventClient.record("SESSION_ENDED", session.getId(), session.getSpaceId(), session.getUserId());
        return session;
    }
}
