package com.example.vivizip.api.common.controller;

import com.example.vivizip.api.common.dto.ApiResponseDto;
import com.example.vivizip.common.annotation.DisableSwaggerSecurity;
import com.example.vivizip.security.jwt.dto.JwtToken;
import com.example.vivizip.security.jwt.service.TokenService;
import com.example.vivizip.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Tag(name = "테스트 용 API")
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestApiController {

    private final TokenService tokenService;

    @DisableSwaggerSecurity // 인증 관련 잠금 없애주는 annotation
    @GetMapping("/health-check")
    public ApiResponseDto<String> healthCheckup() {
        return ApiResponseDto.onSuccess(HttpStatus.OK.toString());
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


