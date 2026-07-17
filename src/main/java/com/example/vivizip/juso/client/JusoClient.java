package com.example.vivizip.juso.client;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.juso.dto.JusoSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

// juso.go.kr 도로명주소 검색 API 호출만 담당.
@Slf4j
@Component
public class JusoClient {

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 5000;

    private final RestClient restClient;
    private final String confmKey;

    public JusoClient(@Value("${juso.api.key}") String confmKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MS);

        this.restClient = RestClient.builder()
                .baseUrl("https://www.juso.go.kr")
                .requestFactory(requestFactory)
                .build();
        this.confmKey = confmKey;
    }

    public List<JusoSearchResponse.Juso> search(String keyword) {
        long start = System.currentTimeMillis();
        log.info("[Juso] 검색 시작: keyword={}", keyword);

        JusoSearchResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/addrlink/addrLinkApi.do")
                            .queryParam("confmKey", confmKey)
                            .queryParam("currentPage", 1)
                            .queryParam("countPerPage", 10)
                            .queryParam("keyword", keyword)
                            .queryParam("resultType", "json")
                            .build())
                    .retrieve()
                    .body(JusoSearchResponse.class);
        } catch (RestClientException e) {
            log.error("[Juso] API 호출 오류 ({}ms 경과): {}", System.currentTimeMillis() - start, e.getMessage());
            throw new GeneralException(ErrorStatus.JUSO_API_ERROR);
        }
        log.info("[Juso] HTTP 응답 수신: {}ms 경과", System.currentTimeMillis() - start);

        if (response == null || response.results() == null || response.results().common() == null) {
            log.error("[Juso] 비정상 응답 수신: keyword={}, response={}", keyword, response);
            throw new GeneralException(ErrorStatus.JUSO_API_ERROR);
        }
        String errorCode = response.results().common().errorCode();
        if (!"0".equals(errorCode)) {
            log.error("[Juso] errorCode={} errorMessage={} keyword={}",
                    errorCode, response.results().common().errorMessage(), keyword);
            throw new GeneralException(ErrorStatus.JUSO_API_ERROR);
        }

        List<JusoSearchResponse.Juso> juso = response.results().juso();
        log.info("[Juso] 검색 완료: {}ms 경과, 결과 {}건", System.currentTimeMillis() - start, juso == null ? 0 : juso.size());
        return juso != null ? juso : List.of();
    }
}