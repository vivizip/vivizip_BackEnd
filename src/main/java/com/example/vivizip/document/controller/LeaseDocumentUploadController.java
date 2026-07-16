package com.example.vivizip.document.controller;

import com.example.vivizip.document.dto.BuildingLedgerAnalysisResponse;
import com.example.vivizip.document.service.LeaseDocumentUploadService;
import com.example.vivizip.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(name = "Document Upload", description = "서류 업로드 API")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class LeaseDocumentUploadController {

    private final LeaseDocumentUploadService leaseDocumentUploadService;

    @Operation(
            summary = "건축물대장 업로드 + OCR + AI 분석 통합 실행",
            description = """
                    사용자가 등록한 주소(leaseCaseId)에 건축물대장 파일(여러 장 가능)을 업로드하고,
                    OCR과 AI 분석까지 한 번에 실행해 결과를 반환합니다.

                    **분석 결과(result) 필드**
                    - `issuedDate`: 건축물대장 발급일자
                    - `hasViolation`: 위반건축물 여부
                    - `ownerName`: 건축물 소유자명
                    - `ownershipTransferDate`: 소유권 이전일자
                    - `address`: 건축물대장에 기재된 도로명 주소
                    - `buildingUse`: 건축물대장에 기재된 건축물 용도
                    - `residential`: 주거용이면 `true`, 비주거용이면 `false`, 판단할 수 없으면 `null`
                    - `ownerMatched`: 등기부등본과 소유자명이 일치하면 `true`, 불일치하면 `false`, 비교 전이면 `null`
                    - `addressMatched`: 등기부등본과 주소가 일치하면 `true`, 불일치하면 `false`, 비교 전이면 `null`
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "건축물대장 업로드 및 분석 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = BuildingLedgerAnalysisResponse.class)
            )
    )
    @PostMapping(
            value = "/building-ledger/upload-analyze",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BuildingLedgerAnalysisResponse uploadAndAnalyzeBuildingLedger(
            @AuthenticationPrincipal CustomUserDetails user,
            @Parameter(description = "사용자가 등록한 주소 ID") @RequestParam Long leaseCaseId,
            @Parameter(description = "업로드할 건축물대장 파일 목록 (여러 장 가능)", required = true) @RequestParam("files") List<MultipartFile> files
    ) throws IOException {
        return leaseDocumentUploadService.uploadAndAnalyzeBuildingLedger(user.getUserId(), leaseCaseId, files);
    }
}
