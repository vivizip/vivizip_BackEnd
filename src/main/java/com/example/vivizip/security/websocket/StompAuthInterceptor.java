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
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("WebSocket CONNECT: Authorization 헤더 없음");
                throw new MessageDeliveryException("WebSocket 연결에 JWT 토큰이 필요합니다.");
            }

            String token = authHeader.substring(7);

            try {
                tokenService.validateToken(token);
                Authentication authentication = tokenService.getAuthentication(token);
                accessor.setUser(authentication);
                log.debug("WebSocket CONNECT 인증 완료: {}", authentication.getName());
            } catch (RuntimeException e) {
                log.warn("WebSocket CONNECT 인증 실패: {}", e.getMessage());
                throw new MessageDeliveryException("유효하지 않은 JWT 토큰입니다: " + e.getMessage());
            }
        }

        return message;
    }
}