package com.example.vivizip.chat.service;

import com.example.vivizip.S3.service.S3Service;
import com.example.vivizip.chat.dto.ChatMessageResponse;
import com.example.vivizip.chat.dto.ChatMessageSliceResponse;
import com.example.vivizip.chat.dto.ChatRoomResponse;
import com.example.vivizip.chat.entity.ChatMessage;
import com.example.vivizip.chat.entity.ChatRoom;
import com.example.vivizip.chat.enums.ChatRoomStatus;
import com.example.vivizip.chat.enums.MessageType;
import com.example.vivizip.chat.repository.ChatMessageRepository;
import com.example.vivizip.chat.repository.ChatRoomRepository;
import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.notification.service.NotificationService;
import com.example.vivizip.user.entity.Role;
import com.example.vivizip.user.entity.User;
import com.example.vivizip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessagePersister chatMessagePersister;
    private final NotificationService notificationService;

    // 방 생성 or 기존 방 반환 (role 검증 포함)
    @Transactional
    public ChatRoomResponse createOrGetRoom(Long requesterId, Long targetId) {
        if (requesterId.equals(targetId)) {
            throw new GeneralException(ErrorStatus.CHAT_SELF_NOT_ALLOWED);
        }

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 같은 role끼리 금지
        if (requester.getRole() == target.getRole()) {
            throw new GeneralException(ErrorStatus.CHAT_SAME_ROLE_NOT_ALLOWED);
        }

        // supporter / student 판별
        User supporter = requester.getRole() == Role.SUPPORTER ? requester : target;
        User student   = requester.getRole() == Role.STUDENT   ? requester : target;

        // 기존 방 있으면 반환, 없으면 생성
        ChatRoom room = chatRoomRepository
                .findBySupporterIdAndStudentId(supporter.getId(), student.getId())
                .orElseGet(() -> chatRoomRepository.save(
                        ChatRoom.of(supporter.getId(), student.getId())));

        return ChatRoomResponse.from(room);
    }

    // 매칭 성공 시 채팅방 생성
    @Transactional
    public Long createForMatch(Long matchId, Long studentId, Long supporterId) {
        ChatRoom room = chatRoomRepository.save(
                ChatRoom.forMatch(matchId, supporterId, studentId));
        return room.getId();
    }

    // 매칭 취소 시 채팅방 닫기
    @Transactional
    public void closeByMatchId(Long matchId) {
        chatRoomRepository.findByMatchId(matchId).ifPresent(room -> {
            room.close();
            chatRoomRepository.save(room);
        });
    }

    // matchId로 roomId 조회
    @Transactional(readOnly = true)
    public Long findRoomIdByMatchId(Long matchId) {
        return chatRoomRepository.findByMatchId(matchId)
                .map(ChatRoom::getId)
                .orElse(null);
    }

    // 내 활성 방 목록
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyRooms(Long userId) {
        return chatRoomRepository.findAllByUserIdAndStatus(userId, ChatRoomStatus.ACTIVE).stream()
                .map(ChatRoomResponse::from)
                .toList();
    }

    // 이전 대화 페이징 (커서 기반)
    @Transactional(readOnly = true)
    public ChatMessageSliceResponse getMessages(Long userId, Long roomId, Long cursor, int size) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.CHAT_ROOM_NOT_FOUND));

        // 이 방 참여자만 조회 가능
        if (!room.hasParticipant(userId)) {
            throw new GeneralException(ErrorStatus.CHAT_ACCESS_DENIED);
        }

        PageRequest pageable = PageRequest.of(0, size + 1); // hasNext 판단 위해 +1
        List<ChatMessage> found = (cursor == null)
                ? chatMessageRepository.findRecentByRoomId(roomId, pageable)
                : chatMessageRepository.findByRoomIdBeforeCursor(roomId, cursor, pageable);

        boolean hasNext = found.size() > size;
        if (hasNext) found = found.subList(0, size);

        // DB에선 최신순(id DESC)으로 왔으니, 화면 표시용으로 과거→현재로 뒤집음
        List<ChatMessageResponse> messages = new ArrayList<>(
                found.stream().map(ChatMessageResponse::from).toList());
        Collections.reverse(messages);

        Long nextCursor = found.isEmpty() ? null : found.get(found.size() - 1).getId();

        return new ChatMessageSliceResponse(messages, nextCursor, hasNext);
    }

    // 채팅 이미지 업로드 → S3 저장 후 WebSocket 브로드캐스트
    // @Transactional 제거  (S3 업로드 동안 DB 커넥션 점유 방지)
    public ChatMessageResponse uploadImage(Long userId, Long roomId, MultipartFile file) {
        // 1. 검증 (읽기만, 트랜잭션 불필요)
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.CHAT_ROOM_NOT_FOUND));

        if (!room.hasParticipant(userId)) {
            throw new GeneralException(ErrorStatus.CHAT_ACCESS_DENIED);
        }

        // 2. S3 업로드 — 트랜잭션 밖에서 수행
        String imageUrl = s3Service.uploadPublic(file, "chat");

        // 3. DB 저장 — 별도 빈 호출로 짧은 트랜잭션, 여기서 커밋 완료
        ChatMessageResponse response =
                chatMessagePersister.save(roomId, userId, imageUrl, MessageType.IMAGE);

        // 4. 커밋 후 브로드캐스트
        messagingTemplate.convertAndSend("/sub/chat/" + roomId, response);

        // 5. 미접속 대응용 알림/FCM 푸시 (실시간 브로드캐스트와 별개)
        Long recipientId = room.getStudentId().equals(userId) ? room.getSupporterId() : room.getStudentId();
        String senderName = userRepository.findById(userId).map(User::getName).orElse("");
        notificationService.notifyChatMessage(recipientId, roomId, senderName, "사진을 보냈습니다");

        return response;
    }
}