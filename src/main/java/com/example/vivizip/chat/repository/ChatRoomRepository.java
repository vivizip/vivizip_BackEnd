package com.example.vivizip.chat.repository;

import com.example.vivizip.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findBySupporterIdAndStudentId(Long supporterId, Long studentId);

    // 내가 속한 방 목록 (supporter이거나 student인 방)
    @Query("SELECT r FROM ChatRoom r " +
            "WHERE r.supporterId = :userId OR r.studentId = :userId " +
            "ORDER BY r.createdAt DESC")
    List<ChatRoom> findAllByUserId(@Param("userId") Long userId);
}
