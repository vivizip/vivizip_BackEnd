package com.example.vivizip.api.common.controller;

import com.example.vivizip.security.jwt.dto.JwtToken;
import com.example.vivizip.security.jwt.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
//@Profile("local")
public class TestApiController {

    private final TokenService tokenService;

    /** 서버 상태 확인 */
    @GetMapping("/health-check")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }

    /**
     * [로컬 전용] 이메일로 JWT 즉시 발급
     * 카카오 로그인 없이 토큰 흐름 전체를 테스트할 때 사용
     * 예) POST /api/v1/test/token?email=test@example.com
     */
    @PostMapping("/token")
    public ResponseEntity<JwtToken> issueTestToken(@RequestParam String email) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                email, "", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        return ResponseEntity.ok(tokenService.generateToken(authentication));
    }
}
