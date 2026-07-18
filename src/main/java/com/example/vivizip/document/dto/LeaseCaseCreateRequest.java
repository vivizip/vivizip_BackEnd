package com.example.vivizip.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record LeaseCaseCreateRequest(
        @Schema(description = "임대차 케이스 이름. 비워두면 \"우리집\"으로 자동 채워짐", example = "신촌 원룸")
        @Size(max = 100, message = "name은 100자 이하여야 합니다.")
        String name,

        @Schema(description = "도로명 주소", example = "서울특별시 마포구 백범로 35")
        @Size(max = 255, message = "roadAddress는 255자 이하여야 합니다.")
        String roadAddress,

        @Schema(description = "상세 주소", example = "101동 202호")
        @Size(max = 255, message = "detailAddress는 255자 이하여야 합니다.")
        String detailAddress
) {
}
