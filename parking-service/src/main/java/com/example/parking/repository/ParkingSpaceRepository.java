package com.example.parking.repository;

import com.example.parking.model.ParkingSpace;
import com.example.parking.model.ParkingSpaceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkingSpaceRepository
        extends JpaRepository<ParkingSpace, Long> {

    List<ParkingSpace> findByStatus(ParkingSpaceStatus status);
}
