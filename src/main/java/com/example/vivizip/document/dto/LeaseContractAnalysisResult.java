package com.example.vivizip.document.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// 계약서(임대차) 검토 결과. LlmJsonExtractor가 OpenAI 응답을 이 타입으로 파싱 + 검증한다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record LeaseContractAnalysisResult(
        @NotBlank String summary,
        @NotNull List<@Valid RiskClause> riskClauses,
        @NotNull List<@NotBlank String> checklist
) implements AnalysisResult {

    public record RiskClause(
            @NotBlank String clause,
            @NotBlank String riskLevel, // HIGH / MEDIUM / LOW
            @NotBlank String reason
    ) {
    }
}
