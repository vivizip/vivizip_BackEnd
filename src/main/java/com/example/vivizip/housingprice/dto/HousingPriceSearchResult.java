package com.example.vivizip.housingprice.dto;

import com.example.vivizip.housingprice.entity.HousingPrice;
import com.example.vivizip.housingprice.entity.IndividualHousePrice;

import java.math.BigDecimal;

// 공동주택(housing_price)/개별주택(individual_house_price) 조회 결과를 하나의 형태로 합친 응답.
// 개별주택은 complexNm/buildingNm/unitNm 개념이 없어 null이다.
public record HousingPriceSearchResult(
        HousingType type,
        String address,
        String complexNm,
        String buildingNm,
        String unitNm,
        BigDecimal area,
        Long price
) {
    public static HousingPriceSearchResult from(HousingPrice entity) {
        return new HousingPriceSearchResult(
                HousingType.APARTMENT,
                entity.getRoadAddress(),
                entity.getComplexNm(),
                entity.getBuildingNm(),
                entity.getUnitNm(),
                entity.getExclusiveArea(),
                entity.getOfficialPrice());
    }

    public static HousingPriceSearchResult from(IndividualHousePrice entity) {
        String address = entity.getLegalDongNm() == null || entity.getJibun() == null
                ? null
                : entity.getLegalDongNm() + " " + entity.getJibun();
        return new HousingPriceSearchResult(
                HousingType.INDIVIDUAL,
                address,
                null,
                null,
                null,
                entity.getTotalArea(),
                entity.getHousePrice());
    }
}