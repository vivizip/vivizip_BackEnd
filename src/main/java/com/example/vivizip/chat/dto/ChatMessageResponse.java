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
        LocalDateTime createdAt,
        // 내가 보낸 메시지를 상대방이 읽었는지 여부.
        // 상대방의 lastReadMessageId >= 이 메시지 id면 true.
        // 상대방이 보낸 메시지거나 읽음 정보가 없으면 null.
        Boolean isRead
) {
    // 읽음 정보 없이 생성 (WebSocket 실시간 전송 시)
    public static ChatMessageResponse from(ChatMessage m) {
        return new ChatMessageResponse(
                m.getId(), m.getRoomId(), m.getSenderId(),
                m.getContent(), m.getType(), m.getCreatedAt(), null
        );
    }

    // 읽음 여부 포함 생성 (REST 조회 시)
    public static ChatMessageResponse from(ChatMessage m, Long myUserId, Long counterpartLastReadId) {
        Boolean isRead = null;
        if (m.getSenderId().equals(myUserId)) {
            // 내가 보낸 메시지: 상대방이 읽었는지 판단
            isRead = counterpartLastReadId != null && counterpartLastReadId >= m.getId();
        }
        return new ChatMessageResponse(
                m.getId(), m.getRoomId(), m.getSenderId(),
                m.getContent(), m.getType(), m.getCreatedAt(), isRead
        );
    }
}