package com.example.vivizip.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(
        @NotBlank(message = "refreshToken은 필수입니다.")
        String refreshToken
) {}