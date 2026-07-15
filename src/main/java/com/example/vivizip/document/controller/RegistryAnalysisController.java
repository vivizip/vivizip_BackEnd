package com.example.vivizip.document.controller;

import com.example.vivizip.document.dto.DocumentAnalysisRequest;
import com.example.vivizip.document.dto.RegistryAnalysisResult;
import com.example.vivizip.document.service.RegistryAnalysisService;
import com.example.vivizip.ocr.service.OcrService;
import com.example.vivizip.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(name = "Document Analysis", description = "서류 AI 분석 API")
@RestController
@RequiredArgsConstructor
public class RegistryAnalysisController {

    private final RegistryAnalysisService registryAnalysisService;
    private final OcrService ocrService;

    @Operation(
            summary = "[테스트] 등기부등본 AI 분석 (텍스트 직접 입력)",
            description = "OCR 텍스트를 직접 입력해 등기부등본을 분석합니다. 테스트 전용 엔드포인트입니다. " +
                    "실제 사용은 이미지 업로드 엔드포인트를 사용하세요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "분석 성공"),
            @ApiResponse(responseCode = "400", description = "서류 타입 불일치 또는 OCR 텍스트 없음"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "서류를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "AI 분석 실패")
    })
    @PostMapping("/api/documents/{documentId}/analysis/registry")
    public RegistryAnalysisResult analyze(
            @AuthenticationPrincipal CustomUserDetails user,
            @Parameter(description = "분석할 서류(lease_document) ID") @PathVariable Long documentId,
            @Valid @RequestBody DocumentAnalysisRequest request
    ) {
        return registryAnalysisService.analyze(user.getUserId(), documentId, request.ocrText());
    }

    @Operation(
            summary = "등기부등본 AI 분석 (이미지 업로드)",
            description = "등기부등본 이미지를 업로드하면 OCR → AI 분석을 한 번에 처리합니다. " +
                    "leaseCaseId로 기존 등기부 서류가 있으면 재사용하고, 없으면 자동 생성합니다. " +
                    "여러 장 업로드 시 페이지를 합쳐서 분석합니다. " +
                    "분석 결과는 reference_baseline에 저장되어 이후 건축물대장 교차검증에 활용됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "분석 성공"),
            @ApiResponse(responseCode = "400", description = "파일 없음"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "계약 건을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "OCR 또는 AI 분석 실패")
    })
    @PostMapping(value = "/api/lease-cases/{leaseCaseId}/analysis/registry/image", consumes = "multipart/form-data")
    public RegistryAnalysisResult analyzeFromImage(
            @AuthenticationPrincipal CustomUserDetails user,
            @Parameter(description = "계약 건(lease_case) ID") @PathVariable Long leaseCaseId,
            @Parameter(description = "등기부등본 이미지 파일 (jpg, png 등, 여러 장 가능)", required = true)
            @RequestParam("files") List<MultipartFile> files
    ) throws IOException {
        String ocrText = ocrService.extractText(files);
        return registryAnalysisService.analyzeByLeaseCaseId(user.getUserId(), leaseCaseId, ocrText);
    }
}
