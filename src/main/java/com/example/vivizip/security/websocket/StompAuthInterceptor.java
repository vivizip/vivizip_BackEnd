package com.example.vivizip.security.websocket;

import com.example.vivizip.security.jwt.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final TokenService tokenService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        // CONNECT 프레임에서만 JWT 검증 (이후 세션에 Authentication이 유지됨)
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 1순위: STOMP 헤더의 Authorization (웹 환경)
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    tokenService.validateToken(token);
                    Authentication authentication = tokenService.getAuthentication(token);
                    accessor.setUser(authentication);
                    log.debug("WebSocket CONNECT 인증 완료 (헤더): {}", authentication.getName());
                    return message;
                } catch (RuntimeException e) {
                    log.warn("WebSocket CONNECT 인증 실패 (헤더): {}", e.getMessage());
                    throw new MessageDeliveryException("유효하지 않은 JWT 토큰입니다: " + e.getMessage());
                }
            }

            // 2순위: 핸드셰이크 시 쿼리 파라미터로 전달된 토큰 (React Native 환경)
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                Authentication authentication = (Authentication) sessionAttributes.get("authentication");
                if (authentication != null) {
                    accessor.setUser(authentication);
                    log.debug("WebSocket CONNECT 인증 완료 (쿼리 파라미터): {}", authentication.getName());
                    return message;
                }
            }

            log.warn("WebSocket CONNECT: 인증 정보 없음");
            throw new MessageDeliveryException("WebSocket 연결에 JWT 토큰이 필요합니다.");
        }

        return message;
    }
}