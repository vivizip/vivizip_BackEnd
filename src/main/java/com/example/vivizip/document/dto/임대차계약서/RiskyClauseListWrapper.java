package com.example.vivizip.document.dto.임대차계약서;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// OpenAI strict mode는 최상위가 object여야 해서 array를 items 필드로 감싼다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record RiskyClauseListWrapper(
        List<RiskyClauseRaw> items
) {
}