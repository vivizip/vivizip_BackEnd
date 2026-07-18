package com.example.vivizip.notification.dto;

import com.example.vivizip.notification.entity.Notification;
import com.example.vivizip.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String body,
        boolean isRead,
        Long linkedResourceId,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.isRead(),
                notification.getLinkedResourceId(),
                notification.getCreatedAt());
    }
}