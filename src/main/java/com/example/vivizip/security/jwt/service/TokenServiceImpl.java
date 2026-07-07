package com.example.vivizip.security.jwt.service;

import com.example.vivizip.auth.service.RedisService;
import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.security.jwt.dto.JwtToken;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    private final Key key;
    private final RedisService redisService;

    public TokenServiceImpl(@Value("${app.jwt.secret}") String keyString,
                            RedisService redisService) {
        byte[] keyBytes = Decoders.BASE64.decode(keyString);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.redisService = redisService;
    }

    @Override
    public JwtToken generateToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();

        // Access Token 30분
        String accessToken = Jwts.builder()
                .setSubject(authentication.getName())
                .claim("auth", authorities)
                .setExpiration(new Date(now + 1800000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // Refresh Token 7일
        String refreshToken = Jwts.builder()
                .setSubject(authentication.getName())
                .setExpiration(new Date(now + 604800000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        redisService.setValue(refreshToken, authentication.getName());

        log.info("토큰 생성 완료: {}", authentication.getName());

        return JwtToken.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .code_expire(parseExpiration(accessToken))
                .refresh_expire(parseExpiration(refreshToken))
                .build();
    }

    @Override
    public JwtToken issueTokens(String refreshToken) {
        if (!existsRefreshToken(refreshToken)) {
            throw new GeneralException(ErrorStatus.AUTH_REFRESH_TOKEN_NOT_FOUND);
        }
        validateToken(refreshToken);

        redisService.deleteValue(refreshToken);

        Claims claims = parseClaims(refreshToken);
        String username = claims.getSubject();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                username, "", Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));

        JwtToken newTokens = generateToken(authentication);
        log.info("토큰 재발급 완료: {}", username);
        return newTokens;
    }

    @Override
    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);

        if (claims.get("auth") == null) {
            throw new GeneralException(ErrorStatus.AUTH_INVALID_TOKEN);
        }

        Collection<? extends GrantedAuthority> authorities = Arrays.stream(claims.get("auth").toString().split(","))
                .filter(auth -> !auth.isBlank())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(claims.getSubject(), "", authorities);
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT Token: {}", e.getMessage());
            throw new GeneralException(ErrorStatus.AUTH_INVALID_TOKEN);
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT Token: {}", e.getMessage());
            throw new GeneralException(ErrorStatus.AUTH_TOKEN_HAS_EXPIRED);
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT Token: {}", e.getMessage());
            throw new GeneralException(ErrorStatus.AUTH_TOKEN_IS_UNSUPPORTED);
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
            throw new GeneralException(ErrorStatus.AUTH_IS_NULL);
        }
    }

    @Override
    public boolean logout(String refreshToken) {
        redisService.deleteValue(refreshToken);
        log.info("로그아웃 완료");
        return true;
    }

    @Override
    public boolean existsRefreshToken(String refreshToken) {
        return redisService.getValue(refreshToken) != null;
    }

    @Override
    public Date parseExpiration(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
        } catch (JwtException e) {
            log.error("토큰 만료시간 파싱 실패: {}", e.getMessage());
            throw new RuntimeException("토큰 파싱 실패");
        }
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}