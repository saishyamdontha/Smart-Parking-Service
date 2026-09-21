package com.example.parking.service;

import com.example.parking.model.ParkingSpace;
import com.example.parking.model.ParkingSpaceStatus;
import com.example.parking.repository.ParkingSpaceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParkingSpaceService {

    private final ParkingSpaceRepository repository;

    public ParkingSpaceService(ParkingSpaceRepository repository) {
        this.repository = repository;
    }

    public List<ParkingSpace> getAvailableSpaces() {
        return repository.findByStatus(ParkingSpaceStatus.AVAILABLE);
    }

    public ParkingSpace getSpace(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Parking space not found"));
    }

    public ParkingSpace occupy(Long id) {

        ParkingSpace space = getSpace(id);

        if (space.getStatus() == ParkingSpaceStatus.OCCUPIED) {
            throw new IllegalStateException(
                    "Parking space is already occupied");
        }

        space.setStatus(ParkingSpaceStatus.OCCUPIED);

        return repository.save(space);
    }

    public ParkingSpace release(Long id) {

        ParkingSpace space = getSpace(id);

        if (space.getStatus() == ParkingSpaceStatus.AVAILABLE) {
            throw new IllegalStateException(
                    "Parking space is already available");
        }

        space.setStatus(ParkingSpaceStatus.AVAILABLE);

        return repository.save(space);
    }
}
