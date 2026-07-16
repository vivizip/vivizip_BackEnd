package com.example.vivizip.document.controller;

import com.example.vivizip.document.dto.LeaseCaseCreateRequest;
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

    @Operation(summary = "내 임대차 케이스 목록 조회", description = "목록 화면에 필요한 leaseCaseId와 name을 최신순으로 반환합니다.")
    @GetMapping
    public List<LeaseCaseSummaryResponse> getMyLeaseCases(
            @AuthenticationPrincipal CustomUserDetails user) {
        return leaseCaseService.getMyLeaseCases(user.getUserId());
    }

    @Operation(summary = "내 임대차 케이스 상세 조회")
    @GetMapping("/{leaseCaseId}")
    public LeaseCaseResponse getMyLeaseCase(
            @AuthenticationPrincipal CustomUserDetails user,
            @Parameter(description = "임대차 케이스 ID") @PathVariable Long leaseCaseId) {
        return leaseCaseService.getMyLeaseCase(user.getUserId(), leaseCaseId);
    }
}
