package com.example.vivizip.document.dto;

import com.example.vivizip.document.entity.AnalysisStatus;
import com.example.vivizip.document.entity.AnalysisType;

public record DocumentAnalysisResponse(
        Long analysisId,
        AnalysisType analysisType,
        AnalysisStatus status,
        AnalysisResult result,
        String failureReason
) {
}