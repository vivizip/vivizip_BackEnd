package com.example.vivizip.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record DocumentReviewRequest(
        @Schema(description = "이미 추출된 서류 원문 텍스트 (MVP: 계약서만 지원, OCR 연동 전)", example = "임대차 계약서\n임대인 ... 임차인 ...")
        @NotBlank(message = "검토할 문서 내용은 필수입니다")
        String documentText
) {
}
