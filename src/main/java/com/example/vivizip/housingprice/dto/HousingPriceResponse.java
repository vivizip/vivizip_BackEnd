package com.example.vivizip.housingprice.dto;

import com.example.vivizip.housingprice.entity.HousingPrice;

import java.math.BigDecimal;

public record HousingPriceResponse(
        String roadAddress,
        String complexNm,
        String buildingNm,
        String unitNm,
        BigDecimal exclusiveArea,
        Long officialPrice
) {
    public static HousingPriceResponse from(HousingPrice entity) {
        return new HousingPriceResponse(
                entity.getRoadAddress(),
                entity.getComplexNm(),
                entity.getBuildingNm(),
                entity.getUnitNm(),
                entity.getExclusiveArea(),
                entity.getOfficialPrice());
    }
}