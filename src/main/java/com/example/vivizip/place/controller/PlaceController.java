package com.example.vivizip.place.controller;

import com.example.vivizip.place.dto.PlaceSearchResponse;
import com.example.vivizip.place.service.PlaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    @Operation(summary = "장소 검색", description = "키워드로 장소를 검색합니다. 좌표를 함께 전달하면 거리(distance)가 계산됩니다.")
    @GetMapping("/search")
    public PlaceSearchResponse searchPlaces(
            @Parameter(description = "검색 키워드", example = "명동역")
            @RequestParam String query,

            @Parameter(description = "중심 경도(longitude)", example = "126.9857")
            @RequestParam(required = false) Double x,

            @Parameter(description = "중심 위도(latitude)", example = "37.5609")
            @RequestParam(required = false) Double y,

            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size,

            @Parameter(description = "정렬 방식 (accuracy | distance)")
            @RequestParam(defaultValue = "accuracy") String sort
    ) {
        return placeService.searchPlaces(query, x, y, page, size, sort);
    }
}
