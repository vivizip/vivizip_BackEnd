package com.example.vivizip.security.jwt.service;

import com.example.vivizip.security.jwt.dto.JwtToken;
import org.springframework.security.core.Authentication;

import java.util.Date;

public interface TokenService {
    JwtToken generateToken(Authentication authentication);
    JwtToken issueTokens(String refreshToken);
    Authentication getAuthentication(String accessToken);
    boolean validateToken(String token);
    boolean logout(String refreshToken);
    boolean existsRefreshToken(String refreshToken);
    Date parseExpiration(String token);
}