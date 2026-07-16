package com.example.vivizip.document.dto;

import com.example.vivizip.document.entity.LeaseCase;

public record LeaseCaseSummaryResponse(
        Long leaseCaseId,
        String name
) {
    public static LeaseCaseSummaryResponse from(LeaseCase leaseCase) {
        return new LeaseCaseSummaryResponse(leaseCase.getId(), leaseCase.getName());
    }
}
