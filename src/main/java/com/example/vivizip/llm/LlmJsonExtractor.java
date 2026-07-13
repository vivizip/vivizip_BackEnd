package com.example.vivizip.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

// "LLM 호출 모듈": 프롬프트 → OpenAI 호출 → 구조화 JSON 파싱 → Bean Validation 검증 → 실패 시 재시도.
// 특정 서류 타입에 의존하지 않으므로 계약서 외 다른 문서 파이프라인에서도 그대로 재사용한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmJsonExtractor {

    private static final int MAX_ATTEMPTS = 3; // 최초 1회 + 재시도 2회

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public <T> T extract(String systemPrompt, String userPrompt, String schemaName, String jsonSchema, Class<T> targetType) {
        String lastError = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String prompt = (lastError == null)
                    ? userPrompt
                    : userPrompt + "\n\n[이전 응답 오류] " + lastError + "\n스키마에 맞는 올바른 JSON으로만 다시 응답하세요.";

            try {
                String rawJson = openAiClient.chatCompletion(systemPrompt, prompt, schemaName, jsonSchema);
                T parsed = objectMapper.readValue(rawJson, targetType);
                validate(parsed);
                return parsed;
            } catch (OpenAiClientException | JsonProcessingException | IllegalStateException e) {
                lastError = e.getMessage();
                log.warn("[LlmJsonExtractor] {}차 시도 실패: {}", attempt, lastError);
            }
        }

        throw new LlmExtractionException("LLM 구조화 응답 추출에 실패했습니다 (마지막 오류: " + lastError + ")");
    }

    private <T> void validate(T parsed) {
        Set<ConstraintViolation<T>> violations = validator.validate(parsed);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException("검증 실패: " + message);
        }
    }
}
