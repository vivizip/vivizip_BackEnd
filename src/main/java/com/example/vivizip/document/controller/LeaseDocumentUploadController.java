package com.example.vivizip.document.controller;

import com.example.vivizip.document.dto.BuildingLedgerAnalysisResponse;
import com.example.vivizip.document.dto.중개대상물.BrokerageDocumentAnalysisResponse;
import com.example.vivizip.document.dto.임대차계약서.LeaseContractAnalysisResponse;
import com.example.vivizip.document.service.BrokerageDocumentAnalysisService;
import com.example.vivizip.document.service.LeaseContractAnalysisService;
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
    private final BrokerageDocumentAnalysisService brokerageDocumentAnalysisService;
    private final LeaseContractAnalysisService leaseContractAnalysisService;

    @Operation(
            summary = "건축물대장 업로드 + OCR + AI 분석 통합 실행",
            description = """
                    사용자가 등록한 주소(leaseCaseId)에 건축물대장 1쪽(표제부) 사진 1장을 업로드하고,
                    OCR과 AI 분석까지 한 번에 실행해 결과를 반환합니다.
                    1쪽에 분석에 필요한 정보가 모두 있고, 뒷장(변동사항 등)에는 과거 이력 문구가 섞여 있어
                    hasViolation 등 판단을 흐릴 수 있어서 1장만 받습니다.

                    **분석 결과(result) 필드**
                    - `issuedDate`: 건축물대장 발급일자
                    - `hasViolation`: 위반건축물 여부
                    - `ownerName`: 건축물 소유자명
                    - `ownershipTransferDate`: 소유권 이전일자
                    - `address`: 건축물대장의 "대지위치" + "지번"을 합친 주소 (도로명주소 아님, 지번 주소)
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
            @Parameter(description = "업로드할 건축물대장 1쪽(표제부) 사진 1장", required = true) @RequestParam("file") MultipartFile file
    ) throws IOException {
        return leaseDocumentUploadService.uploadAndAnalyzeBuildingLedger(user.getUserId(), leaseCaseId, file);
    }

    @Operation(
            summary = "중개대상물 확인·설명서 업로드 + OCR + AI 분석 통합 실행",
            description = """
                    중개대상물 확인·설명서 이미지를 업로드하면 OCR과 AI 분석을 한 번에 실행해 결과를 반환합니다.
                    값(owner, roadAddress, 근저당 유무)은 AI로 추출하고, 하이라이트 박스는 OCR 키워드 검색으로 획득합니다.
                    등기부등본(reference_baseline)이 등록되어 있으면 소유자·주소·근저당 일치 여부를 함께 비교합니다.

                    **응답 필드**
                    - `basicInfo.matchesRegistry`: 등기부와 소유자·주소가 모두 일치하면 true, 하나라도 다르면 false, 등기부 미등록이면 null
                      (주소 비교 시 등기부의 "외N필지", 중개대상물의 "OO호" 같은 꼬리표는 떼고 비교합니다)
                    - `basicInfo.owner`, `basicInfo.roadAddress`: AI가 추출한 값 ("소재지" 원문 그대로, 동/호수 포함)
                    - `mortgage.matchesRegistry`: 등기부의 근저당 유무와 일치하면 true, 다르면 false, 등기부 미등록이면 null
                    - `*.regions`: 프론트에서 원본 이미지 위에 그릴 하이라이트 박스 목록 (좌표는 CLOVA OCR이 반환한 픽셀 값 그대로)
                    - `brokerageFee`: "⑬ 중개보수" 항목의 중개보수 금액(원). 부가세 포함 "계" 금액이 아님. 못 찾으면 null

                    등기부(reference_baseline)가 이미 등록되어 있으면, 이 문서에서 뽑은 매도인·소재지·보증금·월세도
                    reference_baseline에 함께 저장됩니다(응답에는 포함되지 않음).
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "중개대상물 확인·설명서 업로드 및 분석 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = BrokerageDocumentAnalysisResponse.class)
            )
    )
    @PostMapping(
            value = "/brokerage-document/upload-analyze",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BrokerageDocumentAnalysisResponse uploadAndAnalyzeBrokerageDocument(
            @AuthenticationPrincipal CustomUserDetails user,
            @Parameter(description = "사용자가 등록한 주소 ID") @RequestParam Long leaseCaseId,
            @Parameter(description = "업로드할 중개대상물 확인·설명서 파일 목록 (여러 장 가능)", required = true) @RequestParam("files") List<MultipartFile> files
    ) throws IOException {
        return brokerageDocumentAnalysisService.analyze(user.getUserId(), leaseCaseId, files);
    }

    @Operation(
            summary = "임대차계약서 업로드 + OCR + AI 분석 통합 실행",
            description = """
                    임대차계약서 이미지를 업로드하면 OCR과 AI 분석을 한 번에 실행해 결과를 반환합니다.
                    중개대상물 확인·설명서가 이미 등록되어 있으면 소유자·주소·보증금·월세 일치 여부를 함께 비교합니다.
                    분석 결과는 DB에 저장하지 않는 1회성 응답입니다.

                    **basicInfo 필드**
                    - `matchesBrokerageDocument`: 중개대상물과 소유자·주소가 모두 일치하면 true, 하나라도 다르면 false, 중개대상물 미등록이면 null
                    - `owner`: AI가 추출한 임대인 이름
                    - `contractDate`: 계약 체결일 (YYYY.MM.DD)
                    - `roadAddress`: 소재지 도로명주소 (동·호수 포함)
                    - `leaseStartDate`: 임대차 기간 시작일 (YYYY.MM.DD)
                    - `leaseEndDate`: 임대차 기간 종료일 (YYYY.MM.DD)
                    - `regions`: 소재지·임대인 하이라이트 박스

                    **cost 필드**
                    - `deposit`: 보증금 (원)
                    - `monthlyRent`: 월세 (원, 없으면 null)
                    - `depositMatched`: 중개대상물 보증금과 일치 여부 (중개대상물 미등록이면 null)
                    - `monthlyRentMatched`: 중개대상물 월세와 일치 여부 (중개대상물 미등록이면 null)
                    - `depositMessage`: 보증금 불일치 시 세입자가 중개인에게 읽어 말할 확인 질문 (일치하면 null)
                    - `monthlyRentMessage`: 월세 불일치 시 확인 질문 (일치하면 null)
                    - `regions`: 보증금·차임 하이라이트 박스

                    **riskyClauses 필드** (위험 특약 배열, 없으면 빈 배열)
                    - `originalText`: 계약서 원문 그대로
                    - `reason`: 왜 불리한지 한 문장
                    - `suggestion`: 어떻게 바꿀지 구체적 문구 제안
                    - `regions`: 해당 특약 하이라이트 박스
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "임대차계약서 업로드 및 분석 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = LeaseContractAnalysisResponse.class)
            )
    )
    @PostMapping(
            value = "/lease-contract/upload-analyze",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public LeaseContractAnalysisResponse uploadAndAnalyzeLeaseContract(
            @AuthenticationPrincipal CustomUserDetails user,
            @Parameter(description = "사용자가 등록한 주소 ID") @RequestParam Long leaseCaseId,
            @Parameter(description = "업로드할 임대차계약서 파일 목록 (여러 장 가능)", required = true) @RequestParam("files") List<MultipartFile> files
    ) throws IOException {
        return leaseContractAnalysisService.analyze(user.getUserId(), leaseCaseId, files);
    }
}