package com.example.vivizip.llm;

import com.example.vivizip.llm.dto.OpenAiChatMessage;
import com.example.vivizip.llm.dto.OpenAiChatRequest;
import com.example.vivizip.llm.dto.OpenAiChatResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

// OpenAI Chat Completions API 호출만 담당. 파싱/검증/재시도는 LlmJsonExtractor의 책임.
@Slf4j
@Component
public class OpenAiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiClient(@Value("${openai.api-key}") String apiKey,
                         @Value("${openai.model}") String model,
                         ObjectMapper objectMapper) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.model = model;
        this.objectMapper = objectMapper;
    }

    // jsonSchema: JSON Schema 원문 문자열 (Structured Outputs strict 모드로 전송)
    public String chatCompletion(String systemPrompt, String userPrompt, String schemaName, String jsonSchema) {
        JsonNode schemaNode = parseSchema(jsonSchema);

        OpenAiChatRequest request = new OpenAiChatRequest(
                model,
                List.of(
                        new OpenAiChatMessage("system", systemPrompt),
                        new OpenAiChatMessage("user", userPrompt)
                ),
                new OpenAiChatRequest.ResponseFormat(
                        "json_schema",
                        new OpenAiChatRequest.JsonSchema(schemaName, schemaNode, true)
                )
        );

        try {
            OpenAiChatResponse response = restClient.post()
                    .uri("/v1/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(OpenAiChatResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()
                    || response.choices().get(0).message() == null) {
                throw new OpenAiClientException("OpenAI 응답이 비어 있습니다.");
            }
            return response.choices().get(0).message().content();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[OpenAI] 요청 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new OpenAiClientException("OpenAI API 호출 실패: " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            log.error("[OpenAI] API 호출 오류: {}", e.getMessage());
            throw new OpenAiClientException("OpenAI API 호출 오류: " + e.getMessage(), e);
        }
    }

    private JsonNode parseSchema(String jsonSchema) {
        try {
            return objectMapper.readTree(jsonSchema);
        } catch (JsonProcessingException e) {
            throw new OpenAiClientException("JSON 스키마 파싱 실패: " + e.getMessage(), e);
        }
    }
}
