package com.example.vivizip.auth.service;

import com.example.vivizip.auth.dto.KakaoUserResponse;
import com.example.vivizip.auth.dto.LoginResponse;
import com.example.vivizip.auth.kakao.KakaoApiClient;
import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.security.jwt.dto.JwtToken;
import com.example.vivizip.security.jwt.service.TokenService;
import com.example.vivizip.user.entity.Role;
import com.example.vivizip.user.entity.User;
import com.example.vivizip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final TokenService tokenService;
    private final KakaoApiClient kakaoApiClient;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public LoginResponse loginWithKakao(String kakaoAccessToken) {
        log.info("[Auth] 카카오 로그인 시작");

        // 1. 카카오 사용자 정보 조회
        KakaoUserResponse kakaoUser = kakaoApiClient.getUserInfo(kakaoAccessToken);
        log.info("[Auth] 카카오 사용자 확인: kakaoId={}", kakaoUser.id());

        // 2. 이메일 확인
        String subject = kakaoUser.getEmail();
        log.info("[Auth] 이메일: {}", subject != null ? subject : "없음");
        if (subject == null) {
            throw new GeneralException(ErrorStatus.AUTH_OAUTH2_EMAIL_NOT_FOUND_FROM_PROVIDER);
        }

        // 3. 신규/기존 유저 처리
        boolean isNew = !userRepository.findByKakaoId(String.valueOf(kakaoUser.id())).isPresent();
        User user = userRepository.findByKakaoId(String.valueOf(kakaoUser.id()))
                .orElseGet(() -> {
                    log.info("[Auth] 신규 유저 등록: kakaoId={}", kakaoUser.id());
                    return userRepository.save(User.builder()
                            .kakaoId(String.valueOf(kakaoUser.id()))
                            .email(kakaoUser.getEmail())
                            .name(kakaoUser.getName())
                            .profileImage(kakaoUser.getProfileImageUrl())
                            .role(Role.STUDENT)
                            .build());
                });
        log.info("[Auth] 유저 처리 완료: userId={}, isNew={}", user.getId(), isNew);

        // 4. JWT 발급
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                subject, "", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        JwtToken jwtToken = tokenService.generateToken(authentication);
        log.info("[Auth] JWT 발급 완료: userId={}", user.getId());

        return new LoginResponse(
                jwtToken.getAccessToken(),
                jwtToken.getRefreshToken(),
                user.getId(),
                user.getName()
        );
    }

    @Override
    public JwtToken issueTokens(String refreshToken) {
        return tokenService.issueTokens(refreshToken);
    }

    @Override
    public boolean logout(String refreshToken) {
        return tokenService.logout(refreshToken);
    }
}
