package com.example.vivizip.housingprice.service;

import com.example.vivizip.housingprice.dto.HousingPriceSearchResult;
import com.example.vivizip.housingprice.entity.HousingPrice;
import com.example.vivizip.housingprice.entity.IndividualHousePrice;
import com.example.vivizip.housingprice.repository.HousingPriceRepository;
import com.example.vivizip.housingprice.repository.IndividualHousePriceRepository;
import com.example.vivizip.juso.client.JusoClient;
import com.example.vivizip.juso.dto.JusoSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 도로명주소 하나로 juso API를 거쳐 공동주택(housing_price)/개별주택(individual_house_price) 중
// 해당하는 쪽 하나만 조회한다. juso가 알려주는 bdKdcd(공동주택여부)로 분기하고,
// 후보가 여러 건이어도 첫 번째 것 하나만 반환한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class HousingPriceSearchService {

    private static final String BD_KDCD_APARTMENT = "1";

    private final JusoClient jusoClient;
    private final HousingPriceRepository housingPriceRepository;
    private final IndividualHousePriceRepository individualHousePriceRepository;

    @Transactional(readOnly = true)
    public HousingPriceSearchResult searchByRoadAddress(String roadAddress) {
        long start = System.currentTimeMillis();
        log.info("[HousingPriceSearch] 시작: roadAddress={}", roadAddress);

        List<JusoSearchResponse.Juso> jusoResults = jusoClient.search(roadAddress);
        log.info("[HousingPriceSearch] juso 완료: {}ms 경과", System.currentTimeMillis() - start);
        if (jusoResults.isEmpty()) {
            log.info("[HousingPriceSearch] juso 결과 없음, 종료: {}ms 경과", System.currentTimeMillis() - start);
            return null;
        }
        // countPerPage=10으로 여러 후보가 올 수 있지만, 가장 유력한 첫 번째 후보만 쓴다.
        JusoSearchResponse.Juso juso = jusoResults.get(0);
        boolean isApartment = BD_KDCD_APARTMENT.equals(juso.bdKdcd());
        log.info("[HousingPriceSearch] juso 파싱 결과: roadAddr={}, admCd={}, bdKdcd={} (isApartment={})",
                juso.roadAddr(), juso.admCd(), juso.bdKdcd(), isApartment);

        HousingPriceSearchResult result = isApartment
                ? searchApartment(juso.roadAddr())
                : searchIndividualHouse(juso.admCd(), toJibun(juso.lnbrMnnm(), juso.lnbrSlno()));

        log.info("[HousingPriceSearch] 전체 완료: {}ms 경과, found={}", System.currentTimeMillis() - start, result != null);
        return result;
    }

    private HousingPriceSearchResult searchApartment(String roadAddr) {
        if (roadAddr == null) {
            return null;
        }
        List<HousingPrice> matched = housingPriceRepository.findByRoadAddress(roadAddr);
        return matched.isEmpty() ? null : HousingPriceSearchResult.from(matched.get(0));
    }

    private HousingPriceSearchResult searchIndividualHouse(String legalDongCd, String jibun) {
        if (legalDongCd == null || jibun == null) {
            return null;
        }
        List<IndividualHousePrice> matched = individualHousePriceRepository.findByLegalDongCdAndJibun(legalDongCd, jibun);
        return matched.isEmpty() ? null : HousingPriceSearchResult.from(matched.get(0));
    }

    // lnbrSlno(부번)가 "0"이면 본번만, 아니면 "본번-부번" (예: 465,22 -> "465-22" / 858,0 -> "858")
    private String toJibun(String lnbrMnnm, String lnbrSlno) {
        if (lnbrMnnm == null) {
            return null;
        }
        if (lnbrSlno == null || lnbrSlno.isEmpty() || "0".equals(lnbrSlno)) {
            return lnbrMnnm;
        }
        return lnbrMnnm + "-" + lnbrSlno;
    }
}
