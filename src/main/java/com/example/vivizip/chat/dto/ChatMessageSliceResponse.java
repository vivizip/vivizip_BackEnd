package com.example.vivizip.chat.dto;

import java.util.List;

public record ChatMessageSliceResponse(
        List<ChatMessageResponse> messages,  // 과거→현재 순으로 정렬해서 반환
        Long nextCursor,                      // 다음 조회 시 넘길 커서 (null이면 끝)
        boolean hasNext
) {}
