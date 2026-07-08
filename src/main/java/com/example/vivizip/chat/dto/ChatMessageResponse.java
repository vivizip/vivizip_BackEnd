package com.example.vivizip.chat.dto;

import com.example.vivizip.chat.entity.ChatMessage;
import com.example.vivizip.chat.enums.MessageType;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        Long roomId,
        Long senderId,
        String content,
        MessageType type,
        LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage m) {
        return new ChatMessageResponse(
                m.getId(), m.getRoomId(), m.getSenderId(),
                m.getContent(), m.getType(), m.getCreatedAt()
        );
    }
}