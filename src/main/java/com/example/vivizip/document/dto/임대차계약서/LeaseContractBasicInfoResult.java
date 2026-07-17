package com.example.vivizip.document.dto.임대차계약서;

import com.example.vivizip.document.dto.중개대상물.HighlightRegion;

import java.util.List;

public record LeaseContractBasicInfoResult(
        Boolean matchesBrokerageDocument,
        String owner,
        String contractDate,
        String roadAddress,
        String leaseStartDate,
        String leaseEndDate,
        List<HighlightRegion> regions
) {
}