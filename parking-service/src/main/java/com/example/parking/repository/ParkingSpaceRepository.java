package com.example.parking.repository;

import com.example.parking.model.ParkingSpace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ParkingSpaceRepository extends JpaRepository<ParkingSpace, Long> {

    @Modifying
    @Transactional
    @Query("UPDATE ParkingSpace p SET p.status = com.example.parking.model.ParkingSpaceStatus.OCCUPIED " +
           "WHERE p.id = :id AND p.status = com.example.parking.model.ParkingSpaceStatus.AVAILABLE")
    int occupyIfAvailable(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE ParkingSpace p SET p.status = com.example.parking.model.ParkingSpaceStatus.AVAILABLE " +
           "WHERE p.id = :id AND p.status = com.example.parking.model.ParkingSpaceStatus.OCCUPIED")
    int releaseIfOccupied(@Param("id") Long id);
}
