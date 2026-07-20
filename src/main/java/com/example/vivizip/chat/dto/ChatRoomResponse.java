package com.example.vivizip.chat.dto;

import com.example.vivizip.chat.entity.ChatRoom;
import com.example.vivizip.chat.enums.ChatRoomStatus;

import java.time.LocalDateTime;

public record ChatRoomResponse(
        Long roomId,
        Long supporterId,
        Long studentId,
        Long matchId,
        ChatRoomStatus status,
        LocalDateTime createdAt,
        long unreadCount  // 내가 읽지 않은 메시지 수
) {
    // 안읽음 수 없이 생성 (방 생성/조회 등 단순 반환용)
    public static ChatRoomResponse from(ChatRoom room) {
        return new ChatRoomResponse(
                room.getId(), room.getSupporterId(),
                room.getStudentId(), room.getMatchId(),
                room.getStatus(), room.getCreatedAt(), 0L
        );
    }

    // 안읽음 수 포함 생성 (목록 조회용)
    public static ChatRoomResponse from(ChatRoom room, long unreadCount) {
        return new ChatRoomResponse(
                room.getId(), room.getSupporterId(),
                room.getStudentId(), room.getMatchId(),
                room.getStatus(), room.getCreatedAt(), unreadCount
        );
    }
}