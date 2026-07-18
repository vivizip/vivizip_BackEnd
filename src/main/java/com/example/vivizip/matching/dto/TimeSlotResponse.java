package com.example.vivizip.matching.dto;

import com.example.vivizip.matching.entity.DayOfWeekType;
import com.example.vivizip.matching.entity.TimePeriod;
import com.example.vivizip.matching.entity.TimeSlot;

public record TimeSlotResponse(
        DayOfWeekType day,
        TimePeriod period
) {
    public static TimeSlotResponse from(TimeSlot timeSlot) {
        return new TimeSlotResponse(timeSlot.getDay(), timeSlot.getPeriod());
    }
}