package com.example.vivizip.housingprice.controller;

import com.example.vivizip.housingprice.dto.HousingPriceResponse;
import com.example.vivizip.housingprice.dto.HousingPriceSearchResult;
import com.example.vivizip.housingprice.service.HousingPriceSearchService;
import com.example.vivizip.housingprice.service.HousingPriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 다른 기능(예: 매물 분석)이 HousingPriceService/HousingPriceSearchService를 내부에서 호출해 쓰는 게
// 실제 용도이고, 이 엔드포인트 자체는 프론트가 직접 호출하는 정식 API가 아니라 조회 결과를 확인해보기 위한 테스트용이다.
@Tag(name = "Housing Price", description = "[테스트] 공시가격 조회 API")
@RestController
@RequestMapping("/api/housing-prices")
@RequiredArgsConstructor
public class HousingPriceController {

    private final HousingPriceService housingPriceService;
    private final HousingPriceSearchService housingPriceSearchService;

    @Operation(
            summary = "[테스트] 도로명주소+상세주소로 공동주택 공시가격 조회",
            description = """
                    도로명주소가 정확히 일치하는 세대의 공시가격을 반환합니다. 일치하는 데이터가 없으면 null을 반환합니다.
                    detailAddress("101동 1101호"처럼 동/호가 포함된 문자열)를 함께 넘기면 해당 동/호로 좁혀서 찾습니다.
                    동 없이 "1004호"만 있어도 호로 좁혀서 찾고, 동/호를 뽑아낼 수 없는 형식이면 도로명주소 후보 중에서 찾습니다.
                    후보가 여러 건이어도 맨 처음 나온 것 하나만 반환합니다. 공동주택(housing_price)만 다룹니다.
                    """
    )
    @GetMapping
    public HousingPriceResponse search(
            @Parameter(description = "도로명주소", required = true) @RequestParam String roadAddress,
            @Parameter(description = "상세주소 (예: \"101동 1101호\"). 넘기면 해당 동/호로 좁혀서 검색") @RequestParam(required = false) String detailAddress) {
        return housingPriceService.search(roadAddress, detailAddress);
    }

    @Operation(
            summary = "[테스트] 도로명주소로 공동주택/개별주택 공시가격 통합 조회",
            description = """
                    도로명주소 하나로 juso.go.kr API를 통해 공동주택여부(bdKdcd)와 법정동코드·지번을 알아낸 뒤,
                    공동주택이면 housing_price를, 개별주택이면 individual_house_price를 조회합니다(둘 다 조회하지 않음).
                    후보가 여러 건이어도 첫 번째 것 하나만 반환하고, 일치하는 데이터가 없으면 null을 반환합니다.
                    """
    )
    @GetMapping("/search")
    public HousingPriceSearchResult searchByRoadAddress(
            @Parameter(description = "도로명주소", required = true) @RequestParam String roadAddress) {
        return housingPriceSearchService.searchByRoadAddress(roadAddress);
    }
}