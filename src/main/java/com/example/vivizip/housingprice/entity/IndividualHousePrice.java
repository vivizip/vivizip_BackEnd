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

// 개별(단독)주택 공시가격 참조 데이터. 도로명주소가 없어 법정동코드+지번으로만 조회 가능하다.
// housing_price와 동일하게 외부 적재 전용 읽기 전용 테이블 — INSERT/UPDATE/DELETE 금지.
@Getter
@Entity
@Table(name = "individual_house_price")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IndividualHousePrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "legal_dong_cd", length = 10)
    private String legalDongCd;

    @Column(name = "legal_dong_nm", length = 100)
    private String legalDongNm;

    @Column(name = "jibun", length = 30)
    private String jibun;

    @Column(name = "total_area", precision = 14, scale = 2)
    private BigDecimal totalArea;

    @Column(name = "house_price")
    private Long housePrice;
}