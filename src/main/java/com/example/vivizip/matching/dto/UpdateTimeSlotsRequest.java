package com.example.vivizip.matching.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateTimeSlotsRequest(
        @NotEmpty(message = "timeSlots는 최소 1개 이상이어야 합니다.")
        List<@NotNull TimeSlotRequest> timeSlots
) {}