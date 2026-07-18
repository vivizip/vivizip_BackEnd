package com.example.vivizip.notification.service;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.notification.dto.NotificationResponse;
import com.example.vivizip.notification.dto.NotificationSliceResponse;
import com.example.vivizip.notification.dto.UnreadCountResponse;
import com.example.vivizip.notification.entity.FcmToken;
import com.example.vivizip.notification.entity.Notification;
import com.example.vivizip.notification.entity.NotificationType;
import com.example.vivizip.notification.repository.FcmTokenRepository;
import com.example.vivizip.notification.repository.NotificationRepository;
import com.example.vivizip.user.entity.User;
import com.example.vivizip.user.entity.UserStatus;
import com.example.vivizip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

// 알림 insert + (필요 시) FCM 발송을 한 곳에서 처리하는 공통 발송 서비스.
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int CHAT_PREVIEW_MAX_LENGTH = 30;

    private final NotificationRepository notificationRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;
    private final FcmPushService fcmPushService;

    // 커서 기반 슬라이스 조회 (ChatRoomService.getMessages와 동일한 패턴). 최신순 그대로 반환.
    @Transactional(readOnly = true)
    public NotificationSliceResponse getMyNotifications(Long userId, Long cursor, int size) {
        PageRequest pageable = PageRequest.of(0, size + 1); // hasNext 판단 위해 +1
        List<Notification> found = (cursor == null)
                ? notificationRepository.findRecentByUserId(userId, pageable)
                : notificationRepository.findByUserIdBeforeCursor(userId, cursor, pageable);

        boolean hasNext = found.size() > size;
        if (hasNext) found = found.subList(0, size);

        List<NotificationResponse> notifications = found.stream().map(NotificationResponse::from).toList();
        Long nextCursor = found.isEmpty() ? null : found.get(found.size() - 1).getId();

        return new NotificationSliceResponse(notifications, nextCursor, hasNext);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long userId) {
        return UnreadCountResponse.of(notificationRepository.countByUserIdAndIsReadFalse(userId));
    }

    @Transactional
    public UnreadCountResponse readAll(Long userId) {
        notificationRepository.markAllAsRead(userId);
        return UnreadCountResponse.of(0);
    }

    @Transactional
    public void readOne(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOTIFICATION_NOT_FOUND));
        notification.markAsRead();
    }

    // 같은 기기에서 다른 계정으로 로그인하면 토큰 소유자만 바꿔치기(upsert)한다.
    @Transactional
    public void registerFcmToken(Long userId, String token) {
        fcmTokenRepository.findByToken(token)
                .ifPresentOrElse(
                        existing -> existing.reassignTo(userId),
                        () -> fcmTokenRepository.save(FcmToken.create(userId, token)));
    }

    @Transactional
    public void deleteFcmToken(Long userId, String token) {
        fcmTokenRepository.findByToken(token)
                .filter(fcmToken -> fcmToken.getUserId().equals(userId))
                .ifPresent(fcmTokenRepository::delete);
    }

    // 아티클 발행 알림. 실제 "발행" 이벤트를 트리거하는 아티클 기능은 아직 없어 이 메서드만 준비해두고,
    // 아티클 기능이 생기면 발행 처리 로직에서 이 메서드를 호출하면 된다.
    @Transactional
    public void notifyArticleUploaded(String title, String body, Long articleId) {
        List<User> targets = userRepository.findByStatus(UserStatus.ACTIVE);
        List<Notification> notifications = targets.stream()
                .map(user -> Notification.create(user.getId(), NotificationType.ARTICLE_UPLOADED, title, body, articleId))
                .toList();
        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void notifySupporterMatched(Long userId, Long matchId) {
        notificationRepository.save(Notification.create(
                userId,
                NotificationType.SUPPORTER_MATCHED,
                "부메랑 서포터즈 매칭이 완료되었어요",
                "부동산 메이트 '부메랑' 서포터즈 매칭 결과가 발표되었어요. 누구랑 매칭됐을지 확인해보세요.",
                matchId));
    }

    // 채팅 메시지 알림: 알림 목록에 남기는 것 + FCM 푸시(미접속 대응) 둘 다 처리.
    // FCM 발송(외부 네트워크 호출)은 DB 저장 트랜잭션 밖에서 수행한다(ChatRoomService.uploadImage와 동일한 원칙).
    public void notifyChatMessage(Long recipientUserId, Long roomId, String senderName, String messagePreview) {
        String preview = truncate(messagePreview);

        notificationRepository.save(Notification.create(
                recipientUserId, NotificationType.CHAT_MESSAGE, senderName, preview, roomId));

        fcmPushService.sendToUser(recipientUserId, senderName, preview, Map.of(
                "type", NotificationType.CHAT_MESSAGE.name(),
                "roomId", String.valueOf(roomId)
        ));
    }

    private String truncate(String text) {
        if (text == null) return "";
        if (text.length() <= CHAT_PREVIEW_MAX_LENGTH) return text;

        int end = CHAT_PREVIEW_MAX_LENGTH;
        if (Character.isHighSurrogate(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end) + "…";
    }
}