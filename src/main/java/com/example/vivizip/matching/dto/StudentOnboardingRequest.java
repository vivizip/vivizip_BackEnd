package com.example.vivizip.matching.dto;

import com.example.vivizip.user.entity.Gender;
import com.example.vivizip.user.entity.KoreanLevel;
import com.example.vivizip.user.entity.Nationality;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record StudentOnboardingRequest(
        @NotNull(message = "nationality는 필수입니다.")
        Nationality nationality,
        @NotNull(message = "koreanLevel은 필수입니다.")
        KoreanLevel koreanLevel,
        @NotNull(message = "gender는 필수입니다.")
        Gender gender,
        @NotNull(message = "depositBudget은 필수입니다.")
        Integer depositBudget,
        @NotNull(message = "monthlyRentBudget은 필수입니다.")
        Integer monthlyRentBudget,
        @NotEmpty(message = "timeSlots는 최소 1개 이상이어야 합니다.")
        List<@NotNull TimeSlotRequest> timeSlots
) {}