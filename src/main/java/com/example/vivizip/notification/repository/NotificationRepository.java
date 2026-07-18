package com.example.vivizip.notification.repository;

import com.example.vivizip.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// 채팅 메시지 목록(ChatMessageRepository)과 동일한 커서 기반 슬라이스 패턴.
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId ORDER BY n.id DESC")
    List<Notification> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.id < :cursor ORDER BY n.id DESC")
    List<Notification> findByUserIdBeforeCursor(@Param("userId") Long userId,
                                                 @Param("cursor") Long cursor,
                                                 Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") Long userId);
}