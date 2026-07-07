package com.example.vivizip.user.dto;

import com.example.vivizip.user.entity.Language;
import jakarta.validation.constraints.NotNull;

public record UpdateLanguageRequest(
        @NotNull(message = "language는 필수입니다.")
        Language language
) {}