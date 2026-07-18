package com.example.vivizip.matching.dto;

public enum MatchApplicationStatus {
    NOT_APPLIED,         // 온보딩(role) 안 함 — 신청 전
    APPLIED_NOT_MATCHED, // 온보딩은 했지만 MATCHED 매칭 없음 — 신청 후 매칭 전
    MATCHED               // MATCHED 매칭 있음 — 매칭된 후
}