package com.example.vivizip.place.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record KakaoCoord2AddressResponse(
        List<Document> documents,
        Meta meta
) {
    public record Document(
            @JsonProperty("road_address") RoadAddress roadAddress,
            Address address
    ) {}

    public record RoadAddress(
            @JsonProperty("address_name") String addressName
    ) {}

    public record Address(
            @JsonProperty("address_name") String addressName
    ) {}

    public record Meta(
            @JsonProperty("total_count") int totalCount
    ) {}
}