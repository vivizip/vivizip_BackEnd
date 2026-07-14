package com.example.vivizip.document.controller;

import com.example.vivizip.document.dto.DocumentReviewRequest;
import com.example.vivizip.document.dto.DocumentReviewResponse;
import com.example.vivizip.document.service.DocumentReviewService;
import com.example.vivizip.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "서류 검토", description = "AI 기반 서류(계약서 등) 검토 API")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentReviewController {

    private final DocumentReviewService documentReviewService;

    @Operation(summary = "계약서 검토 (MVP: 텍스트 직접 입력)",
            description = "계약서 텍스트를 입력받아 AI로 위험 조항/체크리스트를 분석합니다.")
    @PostMapping("/review")
    public DocumentReviewResponse review(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody DocumentReviewRequest request) {
        return documentReviewService.reviewLeaseContract(user.getUserId(), request.documentText());
    }
}
