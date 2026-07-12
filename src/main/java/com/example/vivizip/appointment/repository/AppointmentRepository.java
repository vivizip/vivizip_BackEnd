package com.example.vivizip.appointment.repository;

import com.example.vivizip.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // 특정 채팅방의 약속 목록 (최신 일정순)
    List<Appointment> findByChatRoomIdOrderByScheduledAtDesc(Long chatRoomId);
}
