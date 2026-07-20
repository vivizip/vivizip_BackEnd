package com.example.vivizip.matching.entity;

import com.example.vivizip.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "schools")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class School extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // 원래 홈페이지 URL용 컬럼(homepage_url)이었으나 어디서도 안 쓰여 로고 이미지 URL로 재활용.
    // DB 컬럼명은 그대로 두고(마이그레이션 없이) 자바 쪽 의미만 바꿈. S3에 미리 올려둔 로고의 공개 URL을 그대로 저장.
    @Column(name = "homepage_url")
    private String logoImageUrl;
}