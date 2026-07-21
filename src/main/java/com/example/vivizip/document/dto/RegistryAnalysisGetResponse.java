package com.example.vivizip.document.dto;

import com.example.vivizip.document.entity.AnalysisStatus;

public record RegistryAnalysisGetResponse(
        Long analysisId,
        AnalysisStatus status,
        RegistryAnalysisResult result,
        String failureReason
) {
}