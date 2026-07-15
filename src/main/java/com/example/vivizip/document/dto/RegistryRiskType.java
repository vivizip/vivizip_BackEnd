package com.example.vivizip.document.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RegistryRiskType {
    PROVISIONAL_REGISTRATION("가등기"),
    TRUST("신탁"),
    SEIZURE("압류"),
    PROVISIONAL_SEIZURE("가압류"),
    AUCTION_START("경매개시결정"),
    LEASE_REGISTRATION("임차권등기명령"),
    JEONSE_RIGHT("전세권");

    private final String label;
}
