package com.example.vivizip.test.redis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RedisTestController {

    private final RedisTemplate<String, String> redisTemplate;

    @GetMapping("/api/redis-test")
    public String testRedis() {
        redisTemplate.opsForValue().set("test-key", "vivizip-redis-ok");
        return redisTemplate.opsForValue().get("test-key");
    }
}
