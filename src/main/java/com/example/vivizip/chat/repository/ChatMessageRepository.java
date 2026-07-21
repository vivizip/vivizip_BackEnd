package com.example.vivizip.chat.repository;

import com.example.vivizip.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 최초 조회: 최신 메시지부터
    @Query("SELECT m FROM ChatMessage m WHERE m.roomId = :roomId ORDER BY m.id DESC")
    List<ChatMessage> findRecentByRoomId(@Param("roomId") Long roomId, Pageable pageable);

    // 커서 이후(더 과거) 조회: cursor보다 id가 작은 것
    @Query("SELECT m FROM ChatMessage m WHERE m.roomId = :roomId AND m.id < :cursor ORDER BY m.id DESC")
    List<ChatMessage> findByRoomIdBeforeCursor(@Param("roomId") Long roomId,
                                               @Param("cursor") Long cursor,
                                               Pageable pageable);

    // 폴링: afterId보다 id가 큰 새 메시지 (오래된 순)
    @Query("SELECT m FROM ChatMessage m WHERE m.roomId = :roomId AND m.id > :afterId ORDER BY m.id ASC")
    List<ChatMessage> findByRoomIdAfterCursor(@Param("roomId") Long roomId,
                                              @Param("afterId") Long afterId,
                                              Pageable pageable);

    // 안읽음 수: lastReadId 이후에 쌓인 메시지 수 (lastReadId가 null이면 전체)
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.roomId = :roomId AND (:lastReadId IS NULL OR m.id > :lastReadId)")
    long countUnread(@Param("roomId") Long roomId, @Param("lastReadId") Long lastReadId);

    void deleteByRoomIdIn(List<Long> roomIds);
}