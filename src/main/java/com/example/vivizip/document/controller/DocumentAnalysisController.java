package com.example.vivizip.document.controller;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.document.dto.BuildingLedgerAnalysisResponse;
import com.example.vivizip.document.dto.BuildingLedgerAnalysisResult;
import com.example.vivizip.document.dto.DocumentAnalysisRequest;
import com.example.vivizip.document.dto.DocumentAnalysisResponse;
import com.example.vivizip.document.dto.중개대상물.BrokerageDocumentAnalysisResponse;
import com.example.vivizip.document.pipeline.BuildingLedgerReviewPipeline;
import com.example.vivizip.document.service.DocumentAnalysisResultService;
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
    private final DocumentAnalysisResultService documentAnalysisResultService;
    private final BuildingLedgerReviewPipeline buildingLedgerReviewPipeline;

    @Operation(
            summary = "건축물대장 분석 결과 조회",
            description = "leaseCaseId로 등록된 건축물대장 중 가장 최근 건의 분석 상태와 결과를 조회합니다. " +
                    "응답 형식은 업로드+분석 API(`/building-ledger/upload-analyze`)와 동일합니다. " +
                    "본인의 임대차 케이스가 아니거나 등록된 건축물대장/분석 결과가 없으면 예외가 발생합니다."
    )
    @GetMapping("/building-ledger/analysis")
    public BuildingLedgerAnalysisResponse getBuildingLedgerResult(
            @AuthenticationPrincipal CustomUserDetails user,
            @Parameter(description = "사용자가 등록한 주소 ID") @RequestParam Long leaseCaseId) {
        return documentAnalysisResultService.getBuildingLedgerResult(user.getUserId(), leaseCaseId);
    }

    @Operation(
            summary = "중개대상물 확인·설명서 분석 결과 조회",
            description = "leaseCaseId로 등록된 중개대상물 확인·설명서 중 가장 최근 건의 분석 결과를 조회합니다. " +
                    "응답 형식은 업로드+분석 API(`/brokerage-document/upload-analyze`)와 동일합니다. " +
                    "본인의 임대차 케이스가 아니거나, 분석이 아직 완료되지 않았거나 결과가 없으면 예외가 발생합니다."
    )
    @GetMapping("/brokerage-document/analysis")
    public BrokerageDocumentAnalysisResponse getBrokerageDocumentResult(
            @AuthenticationPrincipal CustomUserDetails user,
            @Parameter(description = "사용자가 등록한 주소 ID") @RequestParam Long leaseCaseId) {
        return documentAnalysisResultService.getBrokerageDocumentResult(user.getUserId(), leaseCaseId);
    }

    @Operation(
            summary = "[테스트] 서류 AI 분석 요청",
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
            summary = "[테스트] 건축물대장 AI 분석 직접 실행",
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
                result.buildingUse(), result.residential(),
                ownerMatches(result.ownerName(), referenceOwnerName),
                addressMatches(result.address(), referenceAddress)
        );
    }

    // 공동명의 cross-match: 양쪽 이름 목록(쉼표 구분)에서 하나라도 교집합이 있으면 true. reference 없으면 null.
    private Boolean ownerMatches(String extracted, String reference) {
        if (reference == null || reference.isBlank()) return null;
        java.util.Set<String> set = java.util.Arrays.stream(extracted == null ? new String[]{""} : extracted.split(","))
                .map(this::normalize).collect(java.util.stream.Collectors.toSet());
        return java.util.Arrays.stream(reference.split(","))
                .map(this::normalize).anyMatch(set::contains);
    }

    // reference 값이 없으면(빈 입력) 비교 자체를 안 한 것이므로 null. 있으면 true/false.
    private Boolean addressMatches(String extracted, String reference) {
        if (reference == null || reference.isBlank()) return null;
        return normalize(extracted).equals(normalize(reference));
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("[\\s()]", "");
    }
}