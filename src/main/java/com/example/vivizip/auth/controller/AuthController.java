package com.example.vivizip.auth.controller;

import com.example.vivizip.auth.dto.TokenRequest;
import com.example.vivizip.auth.service.AuthService;
import com.example.vivizip.common.annotation.DisableSwaggerSecurity;
import com.example.vivizip.security.jwt.dto.JwtToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @DisableSwaggerSecurity
    @Operation(summary = "토큰 재발급", description = "refreshToken으로 accessToken + refreshToken 재발급 (토큰 로테이션)")
    @PostMapping("/reissue")
    public ResponseEntity<JwtToken> reissueToken(@RequestBody @Valid TokenRequest request) {
        JwtToken newTokens = authService.issueTokens(request.refreshToken());
        log.info("토큰 재발급 완료");
        return ResponseEntity.ok(newTokens);
    }

    @Operation(summary = "로그아웃", description = "refreshToken을 Redis에서 삭제하여 로그아웃 처리")
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody @Valid TokenRequest request) {
        authService.logout(request.refreshToken());
        log.info("로그아웃 완료");
        return ResponseEntity.ok("로그아웃 성공");
    }
}
