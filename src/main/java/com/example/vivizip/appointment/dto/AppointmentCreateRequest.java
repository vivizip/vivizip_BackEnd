package com.example.vivizip.appointment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AppointmentCreateRequest(

        @Schema(description = "약속 일시 (ISO-8601)", example = "2026-07-09T19:00:00")
        @NotNull(message = "약속 일시는 필수입니다")
        @Future(message = "과거 시각으로는 약속을 잡을 수 없습니다")
        LocalDateTime scheduledAt,

        @Schema(description = "장소명", example = "명동역 8번출구")
        @NotBlank(message = "장소명은 필수입니다")
        String placeName,

        @Schema(description = "도로명 주소", example = "서울특별시 중구 수표로 13-1")
        String placeAddress,

        @Schema(description = "위도", example = "37.5609")
        @NotNull(message = "위도는 필수입니다")
        Double latitude,

        @Schema(description = "경도", example = "126.9857")
        @NotNull(message = "경도는 필수입니다")
        Double longitude
) {}
