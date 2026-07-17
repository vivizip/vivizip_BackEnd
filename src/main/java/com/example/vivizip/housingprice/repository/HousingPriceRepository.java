package com.example.vivizip.housingprice.repository;

import com.example.vivizip.housingprice.entity.HousingPrice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// housing_price는 약 1,558만 건짜리 읽기 전용 참조 테이블이라, road_address 완전일치/prefix
// 검색만 제공한다(인덱스: road_address, (sido, sigungu, dongri)). 양쪽 와일드카드 LIKE는 쓰지 않는다.
public interface HousingPriceRepository extends JpaRepository<HousingPrice, Long> {

    List<HousingPrice> findByRoadAddress(String roadAddress);

    List<HousingPrice> findByRoadAddressStartingWith(String roadAddress);

    Page<HousingPrice> findByRoadAddressStartingWith(String roadAddress, Pageable pageable);
}