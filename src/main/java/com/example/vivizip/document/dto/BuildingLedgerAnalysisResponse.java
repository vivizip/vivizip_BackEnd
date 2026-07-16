package com.example.vivizip.document.dto;

import com.example.vivizip.document.entity.AnalysisStatus;

// 건축물대장 전용 분석 응답. DocumentAnalysisResponse.result()는 AnalysisResult 인터페이스라
// 서류 타입마다 실제 응답 형태가 달라 Swagger/프론트에서 구분하기 어렵다. 서류 타입별 API를
// 따로 두기로 하면서, 응답도 타입별로 result를 구체 타입으로 노출한다.
public record BuildingLedgerAnalysisResponse(
        Long analysisId,
        AnalysisStatus status,
        BuildingLedgerAnalysisResult result,
        String failureReason
) {
    public static BuildingLedgerAnalysisResponse from(DocumentAnalysisResponse response) {
        return new BuildingLedgerAnalysisResponse(
                response.analysisId(),
                response.status(),
                (BuildingLedgerAnalysisResult) response.result(),
                response.failureReason()
        );
    }
}
