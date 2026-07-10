package com.example.vivizip.place.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record KakaoPlaceSearchResponse(
        List<Document> documents,
        Meta meta
) {
    public record Document(
            @JsonProperty("place_name") String placeName,
            @JsonProperty("category_name") String categoryName,
            @JsonProperty("road_address_name") String roadAddressName,
            @JsonProperty("address_name") String addressName,
            @JsonProperty("place_url") String placeUrl,
            String x,          // 경도 (문자열로 옴)
            String y,          // 위도
            String distance    // x,y 넘겼을 때만. 아니면 ""
    ) {}

    public record Meta(
            @JsonProperty("total_count") int totalCount,
            @JsonProperty("pageable_count") int pageableCount,
            @JsonProperty("is_end") boolean isEnd
    ) {}
}
