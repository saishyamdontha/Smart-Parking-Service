package com.parking.activity.repository;

import com.parking.activity.model.ActivityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long> {
}
