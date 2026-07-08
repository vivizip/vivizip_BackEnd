package com.example.vivizip.chat.controller;

import com.example.vivizip.chat.dto.ChatMessageRequest;
import com.example.vivizip.chat.dto.ChatMessageResponse;
import com.example.vivizip.chat.entity.ChatMessage;
import com.example.vivizip.chat.enums.MessageType;
import com.example.vivizip.chat.repository.ChatMessageRepository;
import com.example.vivizip.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;

    @MessageMapping("/chat/{roomId}")
    @Transactional
    public void sendMessage(@DestinationVariable Long roomId,
                            ChatMessageRequest request,
                            Principal principal) {
        // setUser(authentication) 했으므로 principal == Authentication
        Authentication authentication = (Authentication) principal;
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long senderId = userDetails.getUserId();

        ChatMessage saved = chatMessageRepository.save(
                ChatMessage.of(roomId, senderId, request.content(), MessageType.TEXT)
        );
        log.info("메시지 저장 - id: {}, roomId: {}, senderId: {}", saved.getId(), roomId, senderId);

        ChatMessageResponse response = ChatMessageResponse.from(saved);
        messagingTemplate.convertAndSend("/sub/chat/" + roomId, response);
    }
}