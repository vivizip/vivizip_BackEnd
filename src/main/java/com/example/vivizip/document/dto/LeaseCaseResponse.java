package com.example.vivizip.document.dto;

import com.example.vivizip.document.entity.ContractStage;
import com.example.vivizip.document.entity.LeaseCase;
import com.example.vivizip.document.entity.LeaseCaseStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record LeaseCaseResponse(
        Long leaseCaseId,
        String name,
        String roadAddress,
        String detailAddress,
        LeaseCaseStatus status,
        @Schema(
                description = """
                        계약 진행 단계
                        - BEFORE_CONTRACT: 계약 전
                        - DURING_CONTRACT: 계약 중
                        - AFTER_CONTRACT: 계약 후

                        단계 변경 조건:
                        - 등기부등본과 건축물대장 분석 완료 시 계약 중
                        - 중개대상물 확인설명서와 임대차계약서 분석 완료 시 계약 후
                        """,
                example = "BEFORE_CONTRACT"
        )
        ContractStage contractStage,
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
                leaseCase.getContractStage(),
                leaseCase.getCreatedAt(),
                leaseCase.getUpdatedAt()
        );
    }
}
