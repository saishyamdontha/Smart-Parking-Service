package com.parking.session.controller;

import com.parking.session.dto.CreateSessionRequest;
import com.parking.session.model.ParkingSession;
import com.parking.session.service.ParkingSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final ParkingSessionService service;

    public SessionController(ParkingSessionService service) {
        this.service = service;
    }

    @GetMapping
    public List<ParkingSession> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ParkingSession getOne(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<ParkingSession> start(@RequestBody CreateSessionRequest request) {
        ParkingSession session = service.start(request.userId(), request.spaceId());
        return ResponseEntity.status(201).body(session);
    }

    @PostMapping("/{id}/end")
    public ParkingSession end(@PathVariable Long id) {
        return service.end(id);
    }
}
