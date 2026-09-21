package com.parking.activity.controller;

import com.parking.activity.dto.ActivityEventRequest;
import com.parking.activity.model.ActivityEvent;
import com.parking.activity.service.ActivityEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class ActivityEventController {

    private final ActivityEventService service;

    public ActivityEventController(ActivityEventService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ActivityEvent> record(@RequestBody ActivityEventRequest request) {
        ActivityEvent event = service.record(request);
        return ResponseEntity.status(201).body(event);
    }

    @GetMapping
    public List<ActivityEvent> getAll() {
        return service.findAll();
    }
}
