package com.example.parking.service;

import com.example.parking.exception.ConflictException;
import com.example.parking.exception.NotFoundException;
import com.example.parking.model.ParkingSpace;
import com.example.parking.repository.ParkingSpaceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParkingSpaceService {

    private final ParkingSpaceRepository repository;

    public ParkingSpaceService(ParkingSpaceRepository repository) {
        this.repository = repository;
    }

    public List<ParkingSpace> findAll() {
        return repository.findAll();
    }

    public ParkingSpace findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Parking space " + id + " not found"));
    }

    public void occupy(Long id) {
        ensureExists(id);
        int updated = repository.occupyIfAvailable(id);
        if (updated == 0) {
            throw new ConflictException("Parking space " + id + " is not AVAILABLE");
        }
    }

    public void release(Long id) {
        ensureExists(id);
        int updated = repository.releaseIfOccupied(id);
        if (updated == 0) {
            throw new ConflictException("Parking space " + id + " is not OCCUPIED");
        }
    }

    private void ensureExists(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Parking space " + id + " not found");
        }
    }
}
