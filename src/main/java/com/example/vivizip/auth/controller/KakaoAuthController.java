package com.example.vivizip.auth.controller;

import com.example.vivizip.auth.dto.KakaoLoginRequest;
import com.example.vivizip.auth.dto.LoginResponse;
import com.example.vivizip.auth.service.AuthService;
import com.example.vivizip.common.annotation.DisableSwaggerSecurity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class KakaoAuthController {

    private final AuthService authService;

    @DisableSwaggerSecurity
    @Operation(summary = "카카오 소셜 로그인", description = "카카오 SDK에서 발급받은 accessToken으로 로그인 및 서비스 JWT 발급")
    @PostMapping("/login/kakao")
    public ResponseEntity<LoginResponse> kakaoLogin(@RequestBody @Valid KakaoLoginRequest request) {
        LoginResponse response = authService.loginWithKakao(request.kakaoAccessToken());
        log.info("카카오 로그인 완료: userId={}", response.userId());
        return ResponseEntity.ok(response);
    }
}