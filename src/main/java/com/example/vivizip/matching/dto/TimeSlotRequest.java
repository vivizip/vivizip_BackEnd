package com.example.vivizip.matching.dto;

import com.example.vivizip.matching.entity.DayOfWeekType;
import com.example.vivizip.matching.entity.TimePeriod;
import jakarta.validation.constraints.NotNull;

public record TimeSlotRequest(
        @NotNull(message = "day는 필수입니다.")
        DayOfWeekType day,
        @NotNull(message = "period는 필수입니다.")
        TimePeriod period
) {}