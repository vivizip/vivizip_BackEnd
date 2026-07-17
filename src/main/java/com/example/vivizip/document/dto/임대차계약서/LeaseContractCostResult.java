package com.example.vivizip.document.dto.임대차계약서;

import com.example.vivizip.document.dto.중개대상물.HighlightRegion;

import java.util.List;

public record LeaseContractCostResult(
        Long deposit,
        Long monthlyRent,
        Boolean depositMatched,
        Boolean monthlyRentMatched,
        String depositMessage,
        String monthlyRentMessage,
        List<HighlightRegion> regions
) {
}