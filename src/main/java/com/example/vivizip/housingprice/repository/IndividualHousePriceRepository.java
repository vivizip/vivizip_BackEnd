package com.example.vivizip.housingprice.repository;

import com.example.vivizip.housingprice.entity.IndividualHousePrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// (legal_dong_cd, jibun) 완전일치 검색만 제공한다(인덱스: (legal_dong_cd, jibun), (legal_dong_nm, jibun)).
public interface IndividualHousePriceRepository extends JpaRepository<IndividualHousePrice, Long> {

    List<IndividualHousePrice> findByLegalDongCdAndJibun(String legalDongCd, String jibun);
}