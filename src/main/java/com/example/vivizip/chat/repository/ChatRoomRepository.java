package com.example.vivizip.chat.repository;

import com.example.vivizip.chat.entity.ChatRoom;
import com.example.vivizip.chat.enums.ChatRoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findBySupporterIdAndStudentId(Long supporterId, Long studentId);

    Optional<ChatRoom> findByMatchId(Long matchId);

    // 내가 속한 활성 방 목록
    @Query("SELECT r FROM ChatRoom r " +
            "WHERE (r.supporterId = :userId OR r.studentId = :userId) " +
            "AND r.status = :status " +
            "ORDER BY r.createdAt DESC")
    List<ChatRoom> findAllByUserIdAndStatus(@Param("userId") Long userId, @Param("status") ChatRoomStatus status);

    // 회원 탈퇴(하드 삭제) 시 상태 무관 전체 방 조회용
    List<ChatRoom> findAllBySupporterIdOrStudentId(Long supporterId, Long studentId);
}