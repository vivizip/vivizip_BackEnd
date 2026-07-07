package com.example.vivizip.config;

import com.example.vivizip.security.websocket.StompAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthInterceptor stompAuthInterceptor;

    /**
     * STOMP 엔드포인트 등록
     * - React Native는 SockJS를 지원하지 않으므로 withSockJS() 미사용
     * - 클라이언트: new Client({ brokerURL: 'ws://host/ws-chat' })
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*");
    }

    /**
     * 메시지 브로커 설정
     * - /topic/chat/{roomId} : 채팅방 구독 (1:1이므로 실질적으로 인원 제한은 서비스 레이어에서)
     * - /app               : 클라이언트가 메시지 발행 시 prefix (예: /app/chat/{roomId})
     * - /user              : convertAndSendToUser() 사용 시 개인 목적지 prefix
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * 인바운드 채널에 JWT 인증 인터셉터 등록
     * STOMP CONNECT 프레임에서 Authorization 헤더를 검증한다.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthInterceptor);
    }
}
