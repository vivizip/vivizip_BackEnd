package com.example.vivizip.document.dto;

import jakarta.validation.constraints.NotBlank;

// OCR은 별도 API(/api/ocr/text)에서 이미 추출된 텍스트를 그대로 받는다.
public record DocumentAnalysisRequest(
        @NotBlank String ocrText
) {
}