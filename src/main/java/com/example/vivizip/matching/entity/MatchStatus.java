package com.example.vivizip.matching.entity;

public enum MatchStatus {
    // 신청했지만 아직 상대(서포터즈)를 찾지 못해 대기 중. supporterId는 null.
    PENDING,
    MATCHED,
    CANCELED
}