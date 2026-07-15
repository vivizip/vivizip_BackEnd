package com.example.vivizip.document.dto;

import com.example.vivizip.document.entity.LeaseCase;
import com.example.vivizip.document.entity.LeaseCaseStatus;

import java.time.LocalDateTime;

public record LeaseCaseResponse(
        Long leaseCaseId,
        String name,
        String roadAddress,
        String detailAddress,
        LeaseCaseStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static LeaseCaseResponse from(LeaseCase leaseCase) {
        return new LeaseCaseResponse(
                leaseCase.getId(),
                leaseCase.getName(),
                leaseCase.getRoadAddress(),
                leaseCase.getDetailAddress(),
                leaseCase.getStatus(),
                leaseCase.getCreatedAt(),
                leaseCase.getUpdatedAt()
        );
    }
}
