package com.example.vivizip.security.jwt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * JWT 토큰 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtToken {
    private String grantType;           // "Bearer"
    private String accessToken;         // 액세스 토큰
    private String refreshToken;        // 리프레시 토큰
    private Date code_expire;           // 액세스 토큰 만료 시간
    private Date refresh_expire;        // 리프레시 토큰 만료 시간
}
