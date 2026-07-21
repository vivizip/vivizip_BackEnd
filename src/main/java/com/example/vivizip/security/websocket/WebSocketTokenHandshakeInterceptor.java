package com.example.vivizip.security.websocket;

import com.example.vivizip.security.jwt.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

// WebSocket 핸드셰이크 시 URL 쿼리 파라미터 ?token=... 으로 JWT를 받아 세션 attributes에 저장.
// React Native는 핸드셰이크 HTTP 요청에 커스텀 헤더를 실을 수 없어서 쿼리 파라미터를 대안으로 사용.
// 저장된 Authentication은 StompAuthInterceptor에서 CONNECT 프레임 처리 시 꺼내 쓴다.
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketTokenHandshakeInterceptor implements HandshakeInterceptor {

    private final TokenService tokenService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");
            if (token != null) {
                try {
                    tokenService.validateToken(token);
                    Authentication authentication = tokenService.getAuthentication(token);
                    attributes.put("authentication", authentication);
                    log.debug("[WS Handshake] 쿼리 파라미터 토큰 인증 성공: {}", authentication.getName());
                } catch (RuntimeException e) {
                    log.warn("[WS Handshake] 쿼리 파라미터 토큰 인증 실패: {}", e.getMessage());
                }
            }
        }
        return true; // 토큰 없거나 유효하지 않아도 핸드셰이크는 허용 (STOMP CONNECT에서 최종 검증)
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}