package com.example.vivizip.document.dto.중개대상물;

import com.example.vivizip.document.dto.AnalysisResult;

public record BrokerageDocumentAnalysisResponse(
        BasicInfoResult basicInfo,
        MortgageResult mortgage,
        LiabilityResult liability,
        Long brokerageFee
) implements AnalysisResult {

    @Override
    public String summary() {
        Boolean basicMatches = basicInfo.matchesRegistry();
        Boolean mortgageMatches = mortgage.matchesRegistry();
        if (basicMatches == null || mortgageMatches == null) {
            return "등기부 대조 전입니다.";
        }
        return (basicMatches && mortgageMatches) ? "등기부 내용과 일치합니다." : "등기부와 다른 점이 있습니다.";
    }
}