package com.example.vivizip.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.example.vivizip.notification.entity.FcmToken;
import com.example.vivizip.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// FirebaseMessaging 빈은 자격증명 미설정 시 없을 수 있어 ObjectProvider로 받는다(FirebaseConfig 참고).
// 토큰이 unregistered(더는 유효하지 않음)로 판정되면 DB에서 제거한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class FcmPushService {

    private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;
    private final FcmTokenRepository fcmTokenRepository;

    public void sendToUser(Long userId, String title, String body, Map<String, String> data) {
        FirebaseMessaging firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
        if (firebaseMessaging == null) {
            log.warn("[FCM] FirebaseMessaging 미설정으로 푸시를 건너뜁니다: userId={}", userId);
            return;
        }

        List<FcmToken> tokens = fcmTokenRepository.findByUserId(userId);
        for (FcmToken fcmToken : tokens) {
            send(firebaseMessaging, fcmToken, title, body, data);
        }
    }

    private void send(FirebaseMessaging firebaseMessaging, FcmToken fcmToken, String title, String body, Map<String, String> data) {
        Message message = Message.builder()
                .setToken(fcmToken.getToken())
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data == null ? Map.of() : data)
                .build();

        try {
            firebaseMessaging.send(message);
        } catch (FirebaseMessagingException e) {
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                log.info("[FCM] 무효 토큰 삭제: userId={}, tokenId={}", fcmToken.getUserId(), fcmToken.getId());
                fcmTokenRepository.delete(fcmToken);
            } else {
                log.warn("[FCM] 발송 실패: userId={}, reason={}", fcmToken.getUserId(), e.getMessage());
            }
        }
    }
}