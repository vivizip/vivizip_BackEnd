package com.example.vivizip.llm;

// 재시도를 모두 소진해도 유효한 구조화 JSON을 얻지 못했을 때 발생하는 도메인 비의존 예외
public class LlmExtractionException extends RuntimeException {
    public LlmExtractionException(String message) {
        super(message);
    }
}
