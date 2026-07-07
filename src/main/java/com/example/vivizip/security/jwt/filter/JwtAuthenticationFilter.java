package com.example.vivizip.security.jwt.filter;

import com.example.vivizip.security.jwt.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = resolveToken(request);

            log.info("[JWT] {} {} | 토큰: {}", request.getMethod(), request.getRequestURI(),
                    token != null ? "있음" : "없음");

            if (token != null && tokenService.validateToken(token)) {
                Authentication authentication = tokenService.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("[JWT] 인증 성공 | email: {}", authentication.getName());
            }
        } catch (RuntimeException e) {
            log.warn("[JWT] 토큰 처리 실패 | {}", e.getMessage());
            SecurityContextHolder.clearContext();
            throw e;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}