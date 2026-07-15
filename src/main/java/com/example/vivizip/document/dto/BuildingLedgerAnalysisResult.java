package com.example.vivizip.document.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// 건축물대장 검토 결과. LlmJsonExtractor가 OpenAI 응답을 이 타입으로 파싱 + 검증한다.
// residential은 buildingUse를 근거로 LLM이 직접 판단하며, 판단 불가 시 null.
// ownerMatched/addressMatched는 LLM이 채우지 않고, reference_baseline과 대조한 뒤 서비스 계층에서 채운다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record BuildingLedgerAnalysisResult(

        @Schema(description = "건축물대장 발급일자", example = "2026.07.14")
        @NotBlank String issuedDate,

        @Schema(description = "위반건축물 여부", example = "true")
        boolean hasViolation,

        @Schema(description = "건축물 소유자명", example = "장영숙")
        @NotBlank String ownerName,

        @Schema(description = "소유권 이전일자", example = "2015.04.28")
        @NotBlank String ownershipTransferDate,

        @Schema(description = "건축물대장에 기재된 도로명 주소")
        @NotBlank String address,

        @Schema(description = "건축물대장에 기재된 건축물 용도")
        @NotBlank String buildingUse,


        @Schema(
                description = "건축물 용도의 주거용 여부. 주거용이면 true, 비주거용이면 false, 판단할 수 없으면 null",
                example = "true",
                nullable = true
        )
        Boolean residential,

        @Schema(
                description = "등기부등본 소유자와 건축물대장 소유자의 일치 여부. 비교 전이면 null",
                example = "true",
                nullable = true
        )
        Boolean ownerMatched,

        @Schema(
                description = "등기부등본 주소와 건축물대장 주소의 일치 여부. 비교 전이면 null",
                example = "true",
                nullable = true
        )
        Boolean addressMatched
) implements AnalysisResult {

    @Override
    public String summary() {
        return hasViolation ? "위반건축물로 등록되어 있습니다." : "위반건축물 사항이 없습니다.";
    }
}