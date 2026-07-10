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
                        if (x != null && y != null) {   // 좌표 있을 때만 (distance 계산용)
                            uriBuilder.queryParam("x", x).queryParam("y", y);
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(KakaoPlaceSearchResponse.class);

            log.info("[KakaoLocal] 장소 검색 성공: count={}",
                    response != null ? response.documents().size() : 0);
            return response;

        } catch (HttpClientErrorException e) {
            log.error("[KakaoLocal] 요청 실패: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new GeneralException(ErrorStatus.KAKAO_PLACE_SEARCH_FAILED);
        } catch (RestClientException e) {
            log.error("[KakaoLocal] API 호출 오류: {}", e.getMessage());
            throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }
}
