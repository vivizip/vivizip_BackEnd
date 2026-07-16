package com.example.vivizip.document.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RiskExplanation(
        @NotBlank String riskType,    // RegistryRiskType enum 이름 (예: PROVISIONAL_SEIZURE)
        @NotBlank String term,        // 한국어 용어 (예: 가압류)
        @NotBlank String explanation  // 외국인을 위한 영문 설명
) {}