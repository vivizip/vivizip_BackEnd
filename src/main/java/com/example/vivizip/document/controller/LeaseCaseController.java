package com.example.vivizip.document.controller;

import com.example.vivizip.document.dto.LeaseCaseCreateRequest;
import com.example.vivizip.document.dto.LeaseCaseProgressResponse;
import com.example.vivizip.document.dto.LeaseCaseResponse;
import com.example.vivizip.document.dto.LeaseCaseSummaryResponse;
import com.example.vivizip.document.service.LeaseCaseService;
import com.example.vivizip.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Lease Case", description = "임대차 케이스 API(사용자가 입력한 주소와 이름 관련 API")
@RestController
@RequestMapping("/api/lease-cases")
@RequiredArgsConstructor
public class LeaseCaseController {

    private final LeaseCaseService leaseCaseService;

    @Operation(summary = "임대차 케이스 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaseCaseResponse create(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody LeaseCaseCreateRequest request) {
        return leaseCaseService.create(user.getUserId(), request);
    }

    @Operation(
            summary = "내 임대차 케이스 목록 조회",
            description = "목록 화면에 필요한 leaseCaseId, name, contractStage를 최신순으로 반환합니다.\n\n" +
                    "contractStage(계약 진행 단계)는 서류 분석이 완료될 때마다 자동으로 갱신됩니다.\n" +
                    "- 등기부등본과 건축물대장 분석이 모두 완료되면 DURING_CONTRACT(계약 중)\n" +
                    "- 중개대상물 확인·설명서와 임대차계약서 분석이 모두 완료되면 AFTER_CONTRACT(계약 후)\n" +
                    "- 한 번 올라간 단계는 재분석 등으로도 이전 단계로 내려가지 않습니다."
    )
    @GetMapping
    public List<LeaseCaseSummaryResponse> getMyLeaseCases(
            @AuthenticationPrincipal CustomUserDetails user) {
        return leaseCaseService.getMyLeaseCases(user.getUserId());
    }

    @Operation(
            summary = "부메랑 진행 과정 조회",
            description = "로그인한 사용자의 전체 진행 단계를 3단계 중 하나로 반환합니다.\n\n" +
                    "- 1(메이트 매칭): 아직 서포터즈와 매칭되지 않음\n" +
                    "- 2(집 구하는 중): 매칭은 됐지만, 등록한 임대차 케이스 중 계약서 검토까지 끝난 게 아직 없음\n" +
                    "- 3(계약서 검토): 가장 최근 등록한 임대차 케이스의 계약서 검토(임대차계약서 분석)까지 끝남\n\n" +
                    "케이스를 여러 개 등록했다면 가장 최근 생성된 것(삭제 제외) 하나만 기준으로 판단합니다."
    )
    @GetMapping("/progress")
    public LeaseCaseProgressResponse getProgress(
            @AuthenticationPrincipal CustomUserDetails user) {
        return leaseCaseService.getProgress(user.getUserId());
    }

    @Operation(
            summary = "내 임대차 케이스 상세 조회",
            description = "응답의 contractStage(계약 진행 단계)는 서류 분석이 완료될 때마다 자동으로 갱신됩니다.\n\n" +
                    "- 등기부등본과 건축물대장 분석이 모두 완료되면 DURING_CONTRACT(계약 중)\n" +
                    "- 중개대상물 확인·설명서와 임대차계약서 분석이 모두 완료되면 AFTER_CONTRACT(계약 후)\n" +
                    "- 한 번 올라간 단계는 재분석 등으로도 이전 단계로 내려가지 않습니다."
    )
    @GetMapping("/{leaseCaseId}")
    public LeaseCaseResponse getMyLeaseCase(
            @AuthenticationPrincipal CustomUserDetails user,
            @Parameter(description = "임대차 케이스 ID") @PathVariable Long leaseCaseId) {
        return leaseCaseService.getMyLeaseCase(user.getUserId(), leaseCaseId);
    }
}
