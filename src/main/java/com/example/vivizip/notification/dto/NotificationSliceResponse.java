package com.example.vivizip.notification.dto;

import java.util.List;

public record NotificationSliceResponse(
        List<NotificationResponse> notifications,  // 과거→현재가 아니라 최신순 그대로 반환
        Long nextCursor,                             // 다음 조회 시 넘길 커서 (null이면 끝)
        boolean hasNext
) {}