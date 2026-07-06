package com.example.vivizip.auth.controller;

import com.example.vivizip.auth.dto.KakaoLoginRequest;
import com.example.vivizip.auth.dto.LoginResponse;
import com.example.vivizip.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class KakaoAuthController {

    private final AuthService authService;

    @PostMapping("/login/kakao")
    public ResponseEntity<LoginResponse> kakaoLogin(@RequestBody @Valid KakaoLoginRequest request) {
        LoginResponse response = authService.loginWithKakao(request.kakaoAccessToken());
        log.info("카카오 로그인 완료: userId={}", response.userId());
        return ResponseEntity.ok(response);
    }
}