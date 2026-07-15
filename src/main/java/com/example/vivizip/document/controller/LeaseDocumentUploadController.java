package com.example.vivizip.document.controller;

import com.example.vivizip.document.dto.BuildingLedgerAnalysisResponse;
import com.example.vivizip.document.service.LeaseDocumentUploadService;
import com.example.vivizip.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
            description = "사용자가 등록한 주소(leaseCaseId)에 건축물대장 파일(여러 장 가능)을 업로드하고, OCR과 AI 분석까지 한 번에 실행해 결과를 반환합니다."
    )
    @PostMapping(value = "/building-ledger/upload-analyze", consumes = "multipart/form-data")
    public BuildingLedgerAnalysisResponse uploadAndAnalyzeBuildingLedger(
            @AuthenticationPrincipal CustomUserDetails user,
            @Parameter(description = "사용자가 등록한 주소 ID") @RequestParam Long leaseCaseId,
            @Parameter(description = "업로드할 건축물대장 파일 목록 (여러 장 가능)", required = true) @RequestParam("files") List<MultipartFile> files
    ) throws IOException {
        return leaseDocumentUploadService.uploadAndAnalyzeBuildingLedger(user.getUserId(), leaseCaseId, files);
    }
}
