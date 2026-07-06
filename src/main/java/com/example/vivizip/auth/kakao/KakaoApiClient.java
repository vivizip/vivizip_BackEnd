package com.example.vivizip.auth.kakao;

import com.example.vivizip.auth.dto.KakaoUserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class KakaoApiClient {

    private final RestClient restClient;

    public KakaoApiClient() {
        this.restClient = RestClient.builder()
                .baseUrl("https://kapi.kakao.com")
                .build();
    }

    public KakaoUserResponse getUserInfo(String kakaoAccessToken) {
        log.info("카카오 사용자 정보 요청");
        return restClient.get()
                .uri("/v2/user/me")
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .retrieve()
                .body(KakaoUserResponse.class);
    }
}