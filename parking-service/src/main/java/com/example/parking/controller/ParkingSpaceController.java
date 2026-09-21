package com.example.parking.controller;

import com.example.parking.model.ParkingSpace;
import com.example.parking.service.ParkingSpaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/parking/spaces")
public class ParkingSpaceController {

    private final ParkingSpaceService service;

    public ParkingSpaceController(ParkingSpaceService service) {
        this.service = service;
    }

    @GetMapping("/available")
    public ResponseEntity<List<ParkingSpace>> getAvailableSpaces() {
        return ResponseEntity.ok(service.getAvailableSpaces());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParkingSpace> getSpace(@PathVariable Long id) {
        return ResponseEntity.ok(service.getSpace(id));
    }

    @PostMapping("/internal/{id}/occupy")
    public ResponseEntity<ParkingSpace> occupy(
            @PathVariable Long id) {

        return ResponseEntity.ok(service.occupy(id));
    }

    @PostMapping("/internal/{id}/release")
    public ResponseEntity<ParkingSpace> release(
            @PathVariable Long id) {

        return ResponseEntity.ok(service.release(id));
    }
}
