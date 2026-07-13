package com.example.vivizip.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record OpenAiChatRequest(
        String model,
        List<OpenAiChatMessage> messages,
        @JsonProperty("response_format") ResponseFormat responseFormat
) {
    public record ResponseFormat(
            String type,
            @JsonProperty("json_schema") JsonSchema jsonSchema
    ) {
    }

    public record JsonSchema(
            String name,
            JsonNode schema,
            boolean strict
    ) {
    }
}
