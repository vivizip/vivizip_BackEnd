package com.example.vivizip.document.controller;

import com.example.vivizip.document.dto.RegistryAnalysisResponse;
import com.example.vivizip.document.service.RegistryAnalysisService;
import com.example.vivizip.ocr.service.OcrService;
import com.example.vivizip.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(name = "Document Upload", description = "서류 업로드 API")
@RestController
@RequiredArgsConstructor
public class DocumentUploadController {

    private final RegistryAnalysisService registryAnalysisService;
    private final OcrService ocrService;

    @Operation(
            summary = "등기부등본 업로드 + OCR + AI 분석 통합 실행",
            description = "등기부등본 이미지를 업로드하면 OCR → AI 분석을 한 번에 처리합니다. " +
                    "leaseCaseId로 기존 등기부 서류가 있으면 재사용하고, 없으면 자동 생성합니다. " +
                    "여러 장 업로드 시 페이지를 합쳐서 분석합니다. " +
                    "분석 결과는 reference_baseline에 저장되어 이후 건축물대장 교차검증에 활용됩니다.\n\n" +
                    "**[200 응답 필드]**\n\n" +
                    "**analysis** (등기부 분석 결과)\n" +
                    "- `propertyAddress` : 부동산 주소 (집합건물은 동·호수 포함)\n" +
                    "- `ownerName` : 등기부상 소유자명\n" +
                    "- `registeredAt` : 소유권 등기 접수일 (YYYY.MM.DD)\n" +
                    "- `issuedAt` : 등기부 발행일 또는 열람일 (YYYY.MM.DD)\n" +
                    "- `hasMortgage` : 근저당권 존재 여부\n" +
                    "- `mortgageMaximumClaimAmount` : 근저당 채권최고액 합산 (원 단위, 없으면 null)\n" +
                    "- `buildingUsage` : 건물 용도 (예: 아파트, 도시형생활주택, 없으면 null)\n" +
                    "- `isResidential` : 주거용 건물 여부\n" +
                    "- `hasSummaryPage` : 주요 등기사항 요약 페이지 포함 여부\n" +
                    "- `riskFlags` : 위험 항목 플래그\n" +
                    "  - `provisionalRegistration` : 가등기\n" +
                    "  - `trust` : 신탁\n" +
                    "  - `seizure` : 압류\n" +
                    "  - `provisionalSeizure` : 가압류\n" +
                    "  - `auctionStart` : 경매개시결정\n" +
                    "  - `leaseRegistration` : 임차권등기명령\n" +
                    "  - `jeonseRight` : 전세권\n\n" +
                    "**riskExplanations** (위험 항목 외국인 설명, riskFlags 중 true인 항목만 포함, 없으면 빈 배열)\n" +
                    "- `riskType` : 위험 항목 enum명 (예: SEIZURE)\n" +
                    "- `term` : 한국어 법률 용어 (예: 압류)\n" +
                    "- `explanation` : 세입자 국적 맞춤 한국어 설명"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "분석 성공"),
            @ApiResponse(responseCode = "400", description = "파일 없음"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "계약 건을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "OCR 또는 AI 분석 실패")
    })
    @PostMapping(value = "/api/documents/registry/upload-analyze", consumes = "multipart/form-data")
    public RegistryAnalysisResponse registryUploadAnalyze(
            @AuthenticationPrincipal CustomUserDetails user,
            @Parameter(description = "계약 건(lease_case) ID", required = true) @RequestParam Long leaseCaseId,
            @Parameter(description = "등기부등본 이미지 파일 (jpg, png 등, 여러 장 가능)", required = true)
            @RequestParam("files") List<MultipartFile> files
    ) throws IOException {
        String ocrText = ocrService.extractText(files);
        return registryAnalysisService.analyzeByLeaseCaseId(user.getUserId(), leaseCaseId, ocrText);
    }
}