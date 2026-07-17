package com.example.vivizip.place.service;

import com.example.vivizip.place.client.KakaoLocalClient;
import com.example.vivizip.place.dto.KakaoCoord2AddressResponse;
import com.example.vivizip.place.dto.KakaoPlaceSearchResponse;
import com.example.vivizip.place.dto.NearestAddressResponse;
import com.example.vivizip.place.dto.PlaceSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final KakaoLocalClient kakaoLocalClient;

    public PlaceSearchResponse searchPlaces(
            String query, Double x, Double y, int page, int size, String sort) {

        KakaoPlaceSearchResponse kakaoResponse =
                kakaoLocalClient.searchByKeyword(query, x, y, page, size, sort);

        List<PlaceSearchResponse.PlaceItem> places = kakaoResponse.documents().stream()
                .map(this::toPlaceItem)
                .toList();

        return new PlaceSearchResponse(places, kakaoResponse.meta().isEnd());
    }

    public NearestAddressResponse getNearestAddress(Double x, Double y) {
        KakaoCoord2AddressResponse response = kakaoLocalClient.coord2address(x, y);
        if (response.documents().isEmpty()) {
            return new NearestAddressResponse(null);
        }
        KakaoCoord2AddressResponse.Document doc = response.documents().get(0);
        // 도로명주소 우선, 없으면 지번주소
        String address = (doc.roadAddress() != null && doc.roadAddress().addressName() != null)
                ? doc.roadAddress().addressName()
                : (doc.address() != null ? doc.address().addressName() : null);
        return new NearestAddressResponse(address);
    }

    private PlaceSearchResponse.PlaceItem toPlaceItem(KakaoPlaceSearchResponse.Document doc) {
        return new PlaceSearchResponse.PlaceItem(
                doc.placeName(),
                extractLastCategory(doc.categoryName()),
                doc.roadAddressName(),
                parseDouble(doc.x()),
                parseDouble(doc.y()),
                parseDistance(doc.distance())
        );
    }

    // "교통,수송 > 지하철,전철 > 수도권4호선" → "수도권4호선"
    private String extractLastCategory(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) return "";
        String[] parts = categoryName.split(">");
        return parts[parts.length - 1].trim();
    }

    private Double parseDouble(String value) {
        return (value == null || value.isBlank()) ? null : Double.parseDouble(value);
    }

    // distance는 좌표 안 넘기면 빈 문자열로 옴 → null 처리
    private Integer parseDistance(String distance) {
        return (distance == null || distance.isBlank()) ? null : Integer.parseInt(distance);
    }
}
