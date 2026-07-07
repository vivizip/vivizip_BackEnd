package com.example.vivizip.auth.service;

import com.example.vivizip.auth.dto.LoginResponse;
import com.example.vivizip.security.jwt.dto.JwtToken;

public interface AuthService {
    LoginResponse loginWithKakao(String kakaoAccessToken);
    JwtToken issueTokens(String refreshToken);
    boolean logout(String refreshToken);
}
