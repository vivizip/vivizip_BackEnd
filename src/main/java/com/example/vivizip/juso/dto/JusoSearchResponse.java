package com.example.vivizip.juso.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// juso.go.kr 도로명주소 API 응답. 실제 응답엔 이보다 훨씬 많은 필드가 오므로 필요한 것만 뽑고 나머지는 무시한다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record JusoSearchResponse(
        Results results
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Results(
            Common common,
            List<Juso> juso
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Common(
            String errorCode,
            String errorMessage
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Juso(
            String roadAddr,   // 도로명주소
            String jibunAddr,  // 지번주소
            String admCd,      // 법정동코드
            String lnbrMnnm,   // 지번 본번
            String lnbrSlno,   // 지번 부번
            String bdKdcd      // 공동주택여부: 1=공동주택, 0=개별주택
    ) {
    }
}