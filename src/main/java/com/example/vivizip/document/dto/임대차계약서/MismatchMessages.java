package com.example.vivizip.document.dto.임대차계약서;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// 금액 불일치 시 LLM이 생성한 세입자 확인용 멘트.
@JsonIgnoreProperties(ignoreUnknown = true)
public record MismatchMessages(
        String depositMessage,
        String monthlyRentMessage
) {
}