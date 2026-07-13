package com.example.vivizip.place.dto;


import java.util.List;

public record PlaceSearchResponse(
        List<PlaceItem> places,
        boolean isEnd
) {
    public record PlaceItem(
            String placeName,       // 명동역 8번출구 서울4호선
            String category,        // 서울4호선 출구번호 (마지막 depth만)
            String roadAddress,     // 서울특별시 중구 수표로 13-1
            Double longitude,       // 경도
            Double latitude,        // 위도
            Integer distance        // 67 (m), 좌표 안 넘기면 null
    ) {}
}
