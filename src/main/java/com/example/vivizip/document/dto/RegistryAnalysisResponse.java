package com.example.vivizip.document.dto;

import java.util.List;

public record RegistryAnalysisResponse(
        RegistryAnalysisResult analysis,
        List<RiskExplanation> riskExplanations  // 리스크 없으면 빈 리스트
) {}