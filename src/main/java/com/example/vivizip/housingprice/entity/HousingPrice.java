package com.example.vivizip.housingprice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// 공시가격 참조 데이터(약 1,558만 건). 외부에서 적재하는 읽기 전용 테이블이라
// 애플리케이션에서는 절대 INSERT/UPDATE/DELETE 하지 않는다. 그래서 BaseEntity(감사 컬럼)도
// 상속하지 않고, setter/생성 팩토리 메서드도 두지 않는다.
@Getter
@Entity
@Table(name = "housing_price")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HousingPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "road_address", length = 300)
    private String roadAddress;

    @Column(name = "legal_dong_cd", length = 10)
    private String legalDongCd;

    @Column(name = "sido", length = 30)
    private String sido;

    @Column(name = "sigungu", length = 30)
    private String sigungu;

    @Column(name = "dongri", length = 30)
    private String dongri;

    @Column(name = "complex_nm", length = 100)
    private String complexNm;

    @Column(name = "building_nm", length = 100)
    private String buildingNm;

    @Column(name = "unit_nm", length = 100)
    private String unitNm;

    @Column(name = "exclusive_area", precision = 10, scale = 2)
    private BigDecimal exclusiveArea;

    @Column(name = "official_price")
    private Long officialPrice;
}