package com.example.vivizip.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Nationality {
    KOREA("한국"),
    VIETNAM("베트남"),
    CHINA("중국"),
    JAPAN("일본"),
    USA("미국"),
    ETC("기타");

    private final String label;
}
