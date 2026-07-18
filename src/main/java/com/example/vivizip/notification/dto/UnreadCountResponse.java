package com.example.vivizip.notification.dto;

public record UnreadCountResponse(
        long unreadCount,
        boolean hasUnread
) {
    public static UnreadCountResponse of(long unreadCount) {
        return new UnreadCountResponse(unreadCount, unreadCount > 0);
    }
}