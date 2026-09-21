package com.parking.session.repository;

import com.parking.session.model.ParkingSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {
}
