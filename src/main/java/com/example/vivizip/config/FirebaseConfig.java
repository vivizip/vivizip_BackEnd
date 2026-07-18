package com.example.vivizip.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

// FIREBASE_CREDENTIALS_BASE64가 아직 설정되지 않았거나 디코딩/파싱에 실패하면, 앱 기동 자체는 막지 않고
// FirebaseMessaging 빈을 등록하지 않는다(null 반환). FcmPushService가 ObjectProvider로 안전하게 처리한다.
@Slf4j
@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseMessaging firebaseMessaging(@Value("${firebase.credentials-base64:}") String credentialsBase64) {
        if (credentialsBase64 == null || credentialsBase64.isBlank()) {
            log.warn("[Firebase] firebase.credentials-base64 미설정 — FCM 푸시 비활성화 상태로 기동합니다.");
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(credentialsBase64.trim());
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(decoded)))
                        .build());
            }
            return FirebaseMessaging.getInstance();
        } catch (IllegalArgumentException | IOException e) {
            log.warn("[Firebase] 자격증명 로드 실패 — FCM 푸시 비활성화 상태로 기동합니다: {}", e.getMessage());
            return null;
        }
    }
}
