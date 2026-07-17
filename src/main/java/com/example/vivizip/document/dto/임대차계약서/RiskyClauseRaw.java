package com.example.vivizip.document.dto.임대차계약서;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// LLM이 판단한 위험 특약 항목 (좌표 없는 원본 결과).
@JsonIgnoreProperties(ignoreUnknown = true)
public record RiskyClauseRaw(
        String originalText,
        String reason,
        String suggestion
) {
}