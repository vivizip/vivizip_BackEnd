package com.example.vivizip.document.dto.중개대상물;

public record BrokerageDocumentAnalysisResponse(
        BasicInfoResult basicInfo,
        MortgageResult mortgage,
        LiabilityResult liability,
        Long brokerageFee
) {
}