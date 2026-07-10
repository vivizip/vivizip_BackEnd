package com.example.vivizip.chat.service;

import com.example.vivizip.chat.dto.ChatMessageResponse;
import com.example.vivizip.chat.entity.ChatMessage;
import com.example.vivizip.chat.enums.MessageType;
import com.example.vivizip.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 저장 전용 빈
@Component
@RequiredArgsConstructor
public class ChatMessagePersister {

    private final ChatMessageRepository chatMessageRepository;

    @Transactional   // ← 다른 빈이라 프록시를 거쳐서 정상 동작
    public ChatMessageResponse save(Long roomId, Long senderId, String content, MessageType type) {
        ChatMessage saved = chatMessageRepository.save(
                ChatMessage.of(roomId, senderId, content, type)
        );
        return ChatMessageResponse.from(saved);
    }
}
