package com.example.vivizip.chat.dto;

// WebSocket으로 브로드캐스트되는 읽음 이벤트.
// 프론트에서 type == "READ"면 lastReadMessageId 이하 메시지의 "1"을 제거한다.
public record ChatReadEvent(
        String type,           // 항상 "READ"
        Long readerId,         // 읽음 처리한 유저 ID
        Long lastReadMessageId // 이 ID 이하의 메시지를 읽었음
) {
    public static ChatReadEvent of(Long readerId, Long lastReadMessageId) {
        return new ChatReadEvent("READ", readerId, lastReadMessageId);
    }
}
