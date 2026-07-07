package com.example.vivizip.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Language {
    KOREAN("한국어"),
    ENGLISH("English");

    private final String label;
}
