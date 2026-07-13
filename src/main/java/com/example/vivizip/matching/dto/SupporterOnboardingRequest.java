package com.example.vivizip.matching.dto;

import com.example.vivizip.user.entity.Gender;
import com.example.vivizip.user.entity.Nationality;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SupporterOnboardingRequest(
        @NotNull(message = "nationality는 필수입니다.")
        Nationality nationality,
        @NotNull(message = "gender는 필수입니다.")
        Gender gender,
        @NotEmpty(message = "timeSlots는 최소 1개 이상이어야 합니다.")
        List<@NotNull TimeSlotRequest> timeSlots
) {}