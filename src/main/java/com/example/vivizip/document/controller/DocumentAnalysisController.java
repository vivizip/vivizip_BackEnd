package com.example.vivizip.document.controller;

import com.example.vivizip.document.dto.BuildingLedgerAnalysisResult;
import com.example.vivizip.document.dto.DocumentAnalysisRequest;
import com.example.vivizip.document.dto.DocumentAnalysisResponse;
import com.example.vivizip.document.pipeline.BuildingLedgerReviewPipeline;
import com.example.vivizip.document.service.DocumentAnalysisService;
import com.example.vivizip.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Document Analysis", description = "서류 AI 분석 API")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentAnalysisController {

    private final DocumentAnalysisService documentAnalysisService;
    private final BuildingLedgerReviewPipeline buildingLedgerReviewPipeline;

    @Operation(
            summary = "서류 AI 분석 요청",
            description = "OCR로 이미 추출된 서류 텍스트(ocrText)를 받아 서류 타입(document_type)에 맞는 AI 파이프라인으로 분석하고 결과를 저장합니다.\n\n" +
                    "등기부와 대조가 필요한 서류(현재 건축물대장)는 reference_baseline과 소유자명/주소를 비교해 ownerMatched/addressMatched를 함께 반환합니다.\n\n" +
                    "현재는 LEASE_CONTRACT, BUILDING_LEDGER 타입만 지원하며, 그 외 타입은 400(DOCUMENT_TYPE_NOT_SUPPORTED)을 반환합니다."
    )
    @PostMapping("/{documentId}/analysis")
    public DocumentAnalysisResponse analyze(
            @AuthenticationPrincipal CustomUserDetails user,
            @Parameter(description = "분석할 서류(lease_document) ID") @PathVariable Long documentId,
            @Valid @RequestBody DocumentAnalysisRequest request
    ) {
        return documentAnalysisService.analyze(user.getUserId(), documentId, request.ocrText());
    }

    @Operation(
            summary = "[디버그] 건축물대장 AI 분석 직접 실행",
            description = "documentId/DB 저장 없이 OCR 텍스트를 요청 바디에 그대로(순수 텍스트) 붙여넣어 건축물대장 pipeline만 실행합니다. " +
                    "JSON이 아니라 순수 텍스트라 줄바꿈을 이스케이프할 필요 없이 그냥 복붙하면 됩니다. " +
                    "referenceOwnerName/referenceAddress 쿼리 파라미터를 비워두면 대조 없이 추출 결과만 반환합니다." +
                    "ownerMatched나 addressMatched가  null이면 등기부등본이 없어서 비교할 수 없는 경우입니다."
    )
    @PostMapping(value = "/building-ledger/debug-analyze", consumes = MediaType.TEXT_PLAIN_VALUE)
    public BuildingLedgerAnalysisResult debugAnalyzeBuildingLedger(
            @Parameter(description = "등기부 기준 소유자명 (선택)") @RequestParam(required = false) String referenceOwnerName,
            @Parameter(description = "등기부 기준 주소 (선택)") @RequestParam(required = false) String referenceAddress,
            @RequestBody String ocrText
    ) {
        BuildingLedgerAnalysisResult result = buildingLedgerReviewPipeline.analyze(ocrText);

        return new BuildingLedgerAnalysisResult(
                result.issuedDate(), result.hasViolation(), result.ownerName(), result.ownershipTransferDate(), result.address(),
                matches(result.ownerName(), referenceOwnerName),
                matches(result.address(), referenceAddress)
        );
    }

    // reference 값이 없으면(빈 입력) 비교 자체를 안 한 것이므로 null. 있으면 true/false.
    private Boolean matches(String extracted, String reference) {
        if (reference == null || reference.isBlank()) {
            return null;
        }
        return normalize(extracted).equals(normalize(reference));
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("[\\s()]", "");
    }
}