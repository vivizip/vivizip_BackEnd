package com.example.vivizip.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        String nickname
) {
}
