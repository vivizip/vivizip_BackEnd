package com.example.vivizip.matching.dto;

public enum MatchApplicationStatus {
    NOT_APPLIED,         // 온보딩 안 함, 혹은 신청한 적 없음/취소 후 재신청 안 함 — 신청 전
    APPLIED_NOT_MATCHED, // 유학생: PENDING 매칭 존재 / 서포터즈: 온보딩만 하고 아직 선택 안 됨 — 신청 후 매칭 전(대기 중)
    MATCHED              // MATCHED 매칭 있음 — 매칭된 후
}