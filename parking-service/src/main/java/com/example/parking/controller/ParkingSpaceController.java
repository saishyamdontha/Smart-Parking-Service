package com.example.parking.controller;

import com.example.parking.model.ParkingSpace;
import com.example.parking.service.ParkingSpaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/spaces")
public class ParkingSpaceController {

    private final ParkingSpaceService service;

    public ParkingSpaceController(ParkingSpaceService service) {
        this.service = service;
    }

    @GetMapping
    public List<ParkingSpace> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ParkingSpace getOne(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping("/{id}/occupy")
    public ResponseEntity<Void> occupy(@PathVariable Long id) {
        service.occupy(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<Void> release(@PathVariable Long id) {
        service.release(id);
        return ResponseEntity.noContent().build();
    }
}
