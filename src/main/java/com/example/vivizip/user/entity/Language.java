package com.example.vivizip.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Language {
    KOREAN("한국어"),
    ENGLISH("English"),
    VIETNAMESE("Tiếng Việt"),
    CHINESE("中文");


    private final String label;
}
