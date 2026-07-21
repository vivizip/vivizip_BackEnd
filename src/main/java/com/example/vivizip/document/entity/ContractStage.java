package com.example.vivizip.document.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        계약 진행 단계
        - BEFORE_CONTRACT: 계약 전
        - DURING_CONTRACT: 계약 중
        - AFTER_CONTRACT: 계약 후

        단계 변경 조건:
        - 등기부등본과 건축물대장 분석 완료 시 계약 중
        - 중개대상물 확인설명서와 임대차계약서 분석 완료 시 계약 후
        """)
public enum ContractStage {
    BEFORE_CONTRACT,
    DURING_CONTRACT,
    AFTER_CONTRACT
}
