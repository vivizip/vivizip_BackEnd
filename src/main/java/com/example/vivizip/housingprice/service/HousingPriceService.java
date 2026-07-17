package com.example.vivizip.housingprice.service;

import com.example.vivizip.housingprice.dto.HousingPriceResponse;
import com.example.vivizip.housingprice.entity.HousingPrice;
import com.example.vivizip.housingprice.repository.HousingPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class HousingPriceService {

    // LeaseCase.detailAddress 표기 형식: "101동 1101호". 동 없이 "1004호"만 있는 경우(단독/다세대 등)도 있다.
    private static final Pattern BUILDING_AND_UNIT = Pattern.compile("(\\d+)\\s*동\\s*(\\d+)\\s*호");
    private static final Pattern UNIT_ONLY = Pattern.compile("(\\d+)\\s*호");

    private final HousingPriceRepository housingPriceRepository;

    @Transactional(readOnly = true)
    public HousingPriceResponse search(String roadAddress) {
        return firstOrNull(housingPriceRepository.findByRoadAddress(roadAddress));
    }

    // detailAddress에서 동/호를 뽑아낼 수 있으면 그 세대로 좁혀서 찾고,
    // 뽑아낼 수 없으면(형식이 안 맞거나 null) roadAddress 전체 후보 중에서 찾는다.
    // 후보가 여러 건이어도 맨 처음 나온 것 하나만 반환한다.
    @Transactional(readOnly = true)
    public HousingPriceResponse search(String roadAddress, String detailAddress) {
        List<HousingPrice> candidates = housingPriceRepository.findByRoadAddress(roadAddress);
        if (detailAddress == null) {
            return firstOrNull(candidates);
        }

        Matcher buildingAndUnit = BUILDING_AND_UNIT.matcher(detailAddress);
        if (buildingAndUnit.find()) {
            String building = buildingAndUnit.group(1);
            String unit = buildingAndUnit.group(2);
            return firstOrNull(candidates.stream()
                    .filter(hp -> matchesDigits(hp.getBuildingNm(), building) && matchesDigits(hp.getUnitNm(), unit))
                    .toList());
        }

        Matcher unitOnly = UNIT_ONLY.matcher(detailAddress);
        if (unitOnly.find()) {
            String unit = unitOnly.group(1);
            return firstOrNull(candidates.stream()
                    .filter(hp -> matchesDigits(hp.getUnitNm(), unit))
                    .toList());
        }

        return firstOrNull(candidates);
    }

    // building_nm/unit_nm 원본 표기가 "101동"/"1"처럼 접미사 유무가 들쭉날쭉해서, 양쪽에서 숫자만 뽑아 비교한다.
    private boolean matchesDigits(String dbValue, String target) {
        return dbValue != null && digitsOnly(dbValue).equals(target);
    }

    private String digitsOnly(String value) {
        return value.replaceAll("\\D", "");
    }

    private HousingPriceResponse firstOrNull(List<HousingPrice> entities) {
        return entities.isEmpty() ? null : HousingPriceResponse.from(entities.get(0));
    }
}