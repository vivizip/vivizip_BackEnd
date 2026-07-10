package com.example.vivizip.chat.controller;

import com.example.vivizip.chat.dto.ChatMessageRequest;
import com.example.vivizip.chat.dto.ChatMessageResponse;
import com.example.vivizip.chat.enums.MessageType;
import com.example.vivizip.chat.service.ChatMessagePersister;
import com.example.vivizip.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessagePersister chatMessagePersister;

    @MessageMapping("/chat/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId,
                            ChatMessageRequest request,
                            Principal principal) {
        Authentication authentication = (Authentication) principal;
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long senderId = userDetails.getUserId();

        ChatMessageResponse response =
                chatMessagePersister.save(roomId, senderId, request.content(), MessageType.TEXT);
        log.info("메시지 저장 - id: {}, roomId: {}, senderId: {}", response.messageId(), roomId, senderId);

        messagingTemplate.convertAndSend("/sub/chat/" + roomId, response);
    }
}