package com.example.vivizip.llm;

// OpenAI API 호출(HTTP/네트워크/응답 형식) 실패를 나타내는 도메인 비의존 예외
public class OpenAiClientException extends RuntimeException {
    public OpenAiClientException(String message) {
        super(message);
    }

    public OpenAiClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
