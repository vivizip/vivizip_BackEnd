package com.example.vivizip.document.dto.임대차계약서;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// 임대차계약서 OCR 텍스트에서 AI(LlmJsonExtractor)가 추출한 값.
// *RawText 필드는 계약서에 적힌 원문 그대로이며, OCR 좌표 검색에 사용된다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record LeaseContractExtractedValues(
        // --- 정규화된 값 (비교·표시용) ---
        String owner,
        String contractDate,
        String roadAddress,
        String leaseStartDate,
        String leaseEndDate,
        Long deposit,
        Long monthlyRent,
        List<String> specialClauses,

        // --- 계약서 원문 그대로 (OCR 좌표 검색용) ---
        String contractDateRawText,
        String roadAddressRawText,
        String leaseStartDateRawText,
        String leaseEndDateRawText,
        String depositRawText,
        String monthlyRentRawText
) {
}