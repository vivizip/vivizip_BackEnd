package com.example.vivizip.chat.controller;

import com.example.vivizip.chat.dto.ChatMessageRequest;
import com.example.vivizip.chat.dto.ChatMessageResponse;
import com.example.vivizip.chat.entity.ChatRoom;
import com.example.vivizip.chat.enums.MessageType;
import com.example.vivizip.chat.repository.ChatRoomRepository;
import com.example.vivizip.chat.service.ChatMessagePersister;
import com.example.vivizip.notification.service.NotificationService;
import com.example.vivizip.security.user.CustomUserDetails;
import com.example.vivizip.user.entity.User;
import com.example.vivizip.user.repository.UserRepository;
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
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

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

        notifyRecipient(roomId, senderId, request.content());
    }

    // 실시간 브로드캐스트(WebSocket)와 별개로, 미접속 대응용 알림/FCM 푸시를 상대방에게 보낸다.
    private void notifyRecipient(Long roomId, Long senderId, String preview) {
        chatRoomRepository.findById(roomId).ifPresent(room -> {
            Long recipientId = recipientOf(room, senderId);
            String senderName = userRepository.findById(senderId)
                    .map(User::getName)
                    .orElse("");
            notificationService.notifyChatMessage(recipientId, roomId, senderName, preview);
        });
    }

    private Long recipientOf(ChatRoom room, Long senderId) {
        return room.getStudentId().equals(senderId) ? room.getSupporterId() : room.getStudentId();
    }
}