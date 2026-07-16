package com.example.vivizip.document.dto.중개대상물;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// 중개대상물 확인·설명서 OCR 텍스트에서 AI(LlmJsonExtractor)가 추출한 값.
// owner/roadAddress/deposit/monthlyRent/brokerageFee는 문서에 없으면 null, documentHasMortgage는 "소유권 외의 권리사항" 기재 여부.
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrokerageDocumentExtractedValues(
        String owner,
        String roadAddress,
        boolean documentHasMortgage,
        Long deposit,
        Long monthlyRent,
        Long brokerageFee
) {
}