package com.example.vivizip.document.dto;

import java.util.List;

public record RegistryAnalysisResponse(
        RegistryAnalysisResult analysis,
        List<RiskExplanation> riskExplanations,  // 리스크 없으면 빈 리스트
        Long marketPrice,          // reference_baseline 주소로 조회한 공시가격. 못 구하면 null
        Double depositRiskRatio    // ((채권최고액 + 내 보증금) / marketPrice) * 100. 계산에 필요한 값이 없으면 null
) {}