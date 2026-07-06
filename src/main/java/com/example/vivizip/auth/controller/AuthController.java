package com.example.vivizip.auth.controller;

import com.example.vivizip.auth.dto.TokenRequest;
import com.example.vivizip.auth.service.AuthService;
import com.example.vivizip.security.jwt.dto.JwtToken;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/reissue")
    public ResponseEntity<JwtToken> reissueToken(@RequestBody @Valid TokenRequest request) {
        JwtToken newTokens = authService.issueTokens(request.refreshToken());
        log.info("토큰 재발급 완료");
        return ResponseEntity.ok(newTokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody @Valid TokenRequest request) {
        authService.logout(request.refreshToken());
        log.info("로그아웃 완료");
        return ResponseEntity.ok("로그아웃 성공");
    }
}