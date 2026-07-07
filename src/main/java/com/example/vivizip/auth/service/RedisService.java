package com.example.vivizip.auth.service;

import com.example.vivizip.common.exception.RedisException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis 토큰 저장소
 * RefreshToken 저장 및 조회
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Redis에 키-값 저장 (7일)
     */
    public void setValue(String key, String value) {
        try {
            redisTemplate.opsForValue().set(key, value, 7, TimeUnit.DAYS);
            log.debug("Redis 저장 완료: {}", key);
        } catch (Exception e) {
            log.error("Redis 저장 실패: {}", e.getMessage());
            throw new RedisException();
        }
    }

    /**
     * Redis에서 값 조회
     */
    public String getValue(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            log.debug("Redis 조회 완료: {}", key);
            return value;
        } catch (Exception e) {
            log.error("Redis 조회 실패: {}", e.getMessage());
            throw new RedisException();
        }
    }

    /**
     * Redis에서 키 삭제
     */
    public void deleteValue(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            if (deleted != null && deleted) {
                log.debug("Redis 삭제 완료: {}", key);
            }
        } catch (Exception e) {
            log.error("Redis 삭제 실패: {}", e.getMessage());
            throw new RedisException();
        }
    }

    /**
     * 키 존재 여부 확인
     */
    public boolean hasKey(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("Redis 존재 확인 실패: {}", e.getMessage());
            return false;
        }
    }
}
