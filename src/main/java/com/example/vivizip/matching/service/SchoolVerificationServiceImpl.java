package com.example.vivizip.matching.service;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.matching.dto.SchoolVerificationConfirmRequest;
import com.example.vivizip.matching.dto.SchoolVerificationSendRequest;
import com.example.vivizip.matching.entity.SchoolEmailDomain;
import com.example.vivizip.matching.repository.SchoolEmailDomainRepository;
import com.example.vivizip.user.entity.User;
import com.example.vivizip.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class SchoolVerificationServiceImpl implements SchoolVerificationService {

    private static final String REDIS_KEY_PREFIX = "school-verification:";
    private static final Duration VERIFICATION_TTL = Duration.ofMinutes(5);

    private final SchoolEmailDomainRepository schoolEmailDomainRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    @Override
    @Transactional(readOnly = true)
    public void sendVerification(Long userId, SchoolVerificationSendRequest request) {
        String domain = extractDomain(request.schoolEmail());

        SchoolEmailDomain schoolEmailDomain = schoolEmailDomainRepository.findByEmailDomain(domain)
                .orElseThrow(() -> new GeneralException(ErrorStatus.SCHOOL_EMAIL_DOMAIN_NOT_SUPPORTED));

        String code = generateVerificationCode();
        SchoolVerificationCode payload = new SchoolVerificationCode(
                schoolEmailDomain.getSchoolId(), request.schoolEmail(), code);

        redisTemplate.opsForValue().set(redisKey(userId), writeValue(payload), VERIFICATION_TTL);
        emailService.sendVerificationCode(request.schoolEmail(), code);
    }

    @Override
    @Transactional
    public void confirmVerification(Long userId, SchoolVerificationConfirmRequest request) {
        String value = redisTemplate.opsForValue().get(redisKey(userId));
        if (value == null) {
            throw new GeneralException(ErrorStatus.SCHOOL_VERIFICATION_CODE_EXPIRED);
        }

        SchoolVerificationCode payload = readValue(value);
        if (!payload.schoolEmail().equals(request.schoolEmail())) {
            throw new GeneralException(ErrorStatus.SCHOOL_VERIFICATION_EMAIL_MISMATCH);
        }
        if (!payload.code().equals(request.code())) {
            throw new GeneralException(ErrorStatus.SCHOOL_VERIFICATION_CODE_MISMATCH);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        user.verifySchool(payload.schoolId());

        redisTemplate.delete(redisKey(userId));
    }

    private String extractDomain(String email) {
        return email.substring(email.indexOf('@') + 1);
    }

    private String generateVerificationCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }

    private String redisKey(Long userId) {
        return REDIS_KEY_PREFIX + userId;
    }

    private String writeValue(SchoolVerificationCode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("인증 정보 직렬화에 실패했습니다.", e);
        }
    }

    private SchoolVerificationCode readValue(String value) {
        try {
            return objectMapper.readValue(value, SchoolVerificationCode.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("인증 정보 역직렬화에 실패했습니다.", e);
        }
    }
}