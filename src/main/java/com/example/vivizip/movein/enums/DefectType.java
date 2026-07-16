package com.example.vivizip.movein.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 하자 칩 종류 (화면의 칩 9종)
 */
@Getter
@RequiredArgsConstructor
public enum DefectType {

    WALLPAPER("벽지"),
    MOLD("곰팡이"),
    TILE_CRACK("타일 깨짐"),
    SUNLIGHT("햇빛"),
    BOILER("보일러"),
    NOISE("소음"),
    SMELL("냄새"),
    FURNITURE("가구"),
    LEAK("누수");

    private final String label;
}
