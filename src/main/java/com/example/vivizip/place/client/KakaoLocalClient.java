package com.example.vivizip.place.client;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.place.dto.KakaoPlaceSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class KakaoLocalClient {

    private final RestClient restClient;

    public KakaoLocalClient(@Value("${kakao.local.api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://dapi.kakao.com")                     // 로그인과 도메인 다름!
                .defaultHeader("Authorization", "KakaoAK " + apiKey)   // Bearer 아님!
                .build();
    }

    public KakaoPlaceSearchResponse searchByKeyword(
            String query, Double x, Double y, int page, int size, String sort) {
        log.info("[KakaoLocal] 장소 검색 요청: query={}", query);
        try {
            KakaoPlaceSearchResponse response = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/v2/local/search/keyword.json")
                                .queryParam("query", query)
                                .queryParam("page", page)
                                .queryParam("size", size)
                                .queryParam("sort", sort);
                        if (x != null && y != null) {
                            uriBuilder.queryParam("x", x).queryParam("y", y);
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(KakaoPlaceSearchResponse.class);

            validateResponse(response, query);

            log.info("[KakaoLocal] 장소 검색 성공: count={}", response.documents().size());
            return response;

        } catch (HttpClientErrorException e) {
            log.error("[KakaoLocal] 요청 실패: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new GeneralException(ErrorStatus.KAKAO_PLACE_SEARCH_FAILED); //카카오가 이상한 응답을 줄 경우 errorstatus
        } catch (RestClientException e) {
            log.error("[KakaoLocal] API 호출 오류: {}", e.getMessage());
            throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }

    // 응답 본문 및 필수 필드 검증 — 비정상 응답은 예외로 전환
    private void validateResponse(KakaoPlaceSearchResponse response, String query) {
        if (response == null || response.documents() == null || response.meta() == null) {
            log.error("[KakaoLocal] 비정상 응답 수신: query={}, response={}", query, response);
            throw new GeneralException(ErrorStatus.KAKAO_PLACE_SEARCH_FAILED);
        }
    }
}

