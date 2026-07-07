package com.example.vivizip.auth.kakao;

import com.example.vivizip.auth.dto.KakaoUserResponse;
import com.example.vivizip.common.exception.InternalServerException;
import com.example.vivizip.common.exception.auth.InvalidKakaoTokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
        log.info("[Kakao] 사용자 정보 요청 시작");
        try {
            KakaoUserResponse response = restClient.get()
                    .uri("/v2/user/me")
                    .header("Authorization", "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .body(KakaoUserResponse.class);
            log.info("[Kakao] 사용자 정보 수신 성공: kakaoId={}", response != null ? response.id() : "null");
            return response;
        } catch (HttpClientErrorException e) {
            log.error("[Kakao] 인증 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new InvalidKakaoTokenException();
        } catch (RestClientException e) {
            log.error("[Kakao] API 호출 오류: {}", e.getMessage());
            throw new InternalServerException();
        }
    }
}
