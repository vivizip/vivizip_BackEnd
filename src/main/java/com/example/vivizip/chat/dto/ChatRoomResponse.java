package com.example.vivizip.chat.dto;

import com.example.vivizip.chat.entity.ChatRoom;

import java.time.LocalDateTime;

public record ChatRoomResponse(
        Long roomId,
        Long supporterId,
        Long studentId,
        LocalDateTime createdAt
) {
    public static ChatRoomResponse from(ChatRoom room) {
        return new ChatRoomResponse(
                room.getId(), room.getSupporterId(),
                room.getStudentId(), room.getCreatedAt()
        );
    }
}