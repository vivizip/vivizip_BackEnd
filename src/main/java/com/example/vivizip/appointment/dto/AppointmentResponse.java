package com.example.vivizip.appointment.dto;

import com.example.vivizip.appointment.entity.Appointment;
import com.example.vivizip.appointment.enums.AppointmentStatus;

import java.time.LocalDateTime;

public record AppointmentResponse(
        Long appointmentId,
        Long chatRoomId,
        Long createdBy,
        LocalDateTime scheduledAt,
        String placeName,
        String placeAddress,
        Double latitude,
        Double longitude,
        AppointmentStatus status
) {
    public static AppointmentResponse from(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getChatRoomId(),
                a.getCreatedBy(),
                a.getScheduledAt(),
                a.getPlaceName(),
                a.getPlaceAddress(),
                a.getLatitude(),
                a.getLongitude(),
                a.getStatus()
        );
    }
}
