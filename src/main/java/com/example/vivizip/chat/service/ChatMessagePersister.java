package com.example.vivizip.chat.service;

import com.example.vivizip.chat.dto.ChatMessageResponse;
import com.example.vivizip.chat.entity.ChatMessage;
import com.example.vivizip.chat.entity.ChatRoom;
import com.example.vivizip.chat.enums.MessageType;
import com.example.vivizip.chat.repository.ChatMessageRepository;
import com.example.vivizip.chat.repository.ChatRoomRepository;
import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 저장 전용 빈
@Component
@RequiredArgsConstructor
public class ChatMessagePersister {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;

    @Transactional
    public ChatMessageResponse save(Long roomId, Long senderId, String content, MessageType type) {
        ChatMessage saved = chatMessageRepository.save(
                ChatMessage.of(roomId, senderId, content, type)
        );
        // 내가 보낸 메시지는 내가 읽은 것 → 발신자 lastReadId 갱신
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.CHAT_ROOM_NOT_FOUND));
        room.updateLastRead(senderId, saved.getId());
        chatRoomRepository.save(room);

        return ChatMessageResponse.from(saved);
    }
}
