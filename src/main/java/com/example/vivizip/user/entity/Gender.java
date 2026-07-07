package com.example.vivizip.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Gender {
    MALE("남성"),
    FEMALE("여성"),
    NOT_SPECIFIED("선택 안함");

    private final String label;
}