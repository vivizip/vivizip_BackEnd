package com.example.vivizip.chat.controller;

import com.example.vivizip.chat.dto.ChatMessageRequest;
import com.example.vivizip.chat.dto.ChatMessageResponse;
import com.example.vivizip.chat.entity.ChatMessage;
import com.example.vivizip.chat.enums.MessageType;
import com.example.vivizip.chat.repository.ChatMessageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;

    @MessageMapping("/chat/{roomId}")
    @Transactional
    public void sendMessage(@DestinationVariable Long roomId,
                            ChatMessageRequest request) {
        // 인증 붙기 전 임시 senderId (다음 단계에서 Principal로 교체)
        Long senderId = request.senderId();

        // 1. DB 저장
        ChatMessage saved = chatMessageRepository.save(
                ChatMessage.of(roomId, senderId, request.content(), MessageType.TEXT)
        );
        log.info("메시지 저장 - id: {}, roomId: {}", saved.getId(), roomId);

        // 2. 구독자에게 브로드캐스트
        ChatMessageResponse response = ChatMessageResponse.from(saved);
        messagingTemplate.convertAndSend("/sub/chat/" + roomId, response);
    }

}