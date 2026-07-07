package com.example.vivizip.chat.repository;

import com.example.vivizip.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 중복방 방지 / 기존방 조회
    Optional<ChatRoom> findBySupporterIdAndStudentId(Long supporterId, Long studentId);
}
