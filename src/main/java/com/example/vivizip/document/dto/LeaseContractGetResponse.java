package com.example.vivizip.document.dto;

import com.example.vivizip.document.dto.임대차계약서.LeaseContractAnalysisResponse;
import com.example.vivizip.document.entity.AnalysisStatus;

public record LeaseContractGetResponse(
        Long analysisId,
        AnalysisStatus status,
        LeaseContractAnalysisResponse result,
        String failureReason
) {
}