package com.parking.activity.service;

import com.parking.activity.dto.ActivityEventRequest;
import com.parking.activity.model.ActivityEvent;
import com.parking.activity.repository.ActivityEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityEventService {

    private final ActivityEventRepository repository;

    public ActivityEventService(ActivityEventRepository repository) {
        this.repository = repository;
    }

    public ActivityEvent record(ActivityEventRequest request) {
        ActivityEvent event = new ActivityEvent(
                request.eventType(), request.sessionId(), request.spaceId(), request.userId());
        return repository.save(event);
    }

    public List<ActivityEvent> findAll() {
        return repository.findAll();
    }
}
