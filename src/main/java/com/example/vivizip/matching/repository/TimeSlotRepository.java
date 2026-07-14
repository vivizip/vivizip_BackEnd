package com.example.vivizip.matching.repository;

import com.example.vivizip.matching.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findByUserId(Long userId);

    List<TimeSlot> findByUserIdIn(List<Long> userIds);

    boolean existsByUserId(Long userId);

    void deleteByUserId(Long userId);
}