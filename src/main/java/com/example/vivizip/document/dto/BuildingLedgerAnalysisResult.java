package com.example.vivizip.document.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

// 건축물대장 검토 결과. LlmJsonExtractor가 OpenAI 응답을 이 타입으로 파싱 + 검증한다.
// ownerMatched/addressMatched는 LLM이 채우지 않고, reference_baseline과 대조한 뒤 서비스 계층에서 채운다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record BuildingLedgerAnalysisResult(
        @NotBlank String issuedDate,
        boolean hasViolation,
        @NotBlank String ownerName,
        @NotBlank String ownershipTransferDate,
        @NotBlank String address,
        Boolean ownerMatched,
        Boolean addressMatched
) implements AnalysisResult {

    @Override
    public String summary() {
        return hasViolation ? "위반건축물로 등록되어 있습니다." : "위반건축물 사항이 없습니다.";
    }
}