package com.example.vivizip.matching.controller;

import com.example.vivizip.matching.dto.MatchResponse;
import com.example.vivizip.matching.dto.RematchRequest;
import com.example.vivizip.matching.dto.SchoolVerificationConfirmRequest;
import com.example.vivizip.matching.dto.SchoolVerificationSendRequest;
import com.example.vivizip.matching.dto.StudentOnboardingRequest;
import com.example.vivizip.matching.dto.SupporterOnboardingRequest;
import com.example.vivizip.matching.service.MatchingService;
import com.example.vivizip.matching.service.SchoolVerificationService;
import com.example.vivizip.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.example.vivizip.consts.StaticVariable.SWAGGER_JWT;

@Tag(name = "Match", description = "유학생-서포터즈 매칭 API")
@SecurityRequirement(name = SWAGGER_JWT)
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchingService matchingService;
    private final SchoolVerificationService schoolVerificationService;

    @Operation(summary = "서포터즈 온보딩", description = "학교 인증이 완료된 사용자가 서포터즈로 등록합니다. 호출 시 role이 SUPPORTER로 설정되며, 국적/성별/가능한 시간대를 등록합니다. 기존에 등록된 시간대는 이번 요청 목록으로 전체 교체됩니다.")
    @PostMapping("/onboarding/supporter")
    public ResponseEntity<Void> onboardSupporter(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid SupporterOnboardingRequest request) {
        matchingService.onboardSupporter(userDetails.getUserId(), request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "유학생 온보딩", description = "학교 인증이 완료된 사용자가 유학생으로 등록합니다. 호출 시 role이 STUDENT로 설정되며, 국적/성별/한국어 수준/예산/가능한 시간대를 등록합니다. 기존에 등록된 시간대는 이번 요청 목록으로 전체 교체됩니다.")
    @PostMapping("/onboarding/student")
    public ResponseEntity<Void> onboardStudent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid StudentOnboardingRequest request) {
        matchingService.onboardStudent(userDetails.getUserId(), request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "학교 이메일 인증 코드 발송", description = "학교 이메일 도메인이 지원 대상인지 확인 후, 6자리 인증 코드를 생성해 Redis에 5분간 저장하고 이메일로 발송합니다.")
    @PostMapping("/school-verification/send")
    public ResponseEntity<Void> sendSchoolVerification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid SchoolVerificationSendRequest request) {
        schoolVerificationService.sendVerification(userDetails.getUserId(), request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "학교 이메일 인증 코드 확인", description = "발송된 인증 코드와 이메일을 검증하고, 일치하면 사용자의 schoolId와 schoolVerified를 갱신합니다.")
    @PostMapping("/school-verification/confirm")
    public ResponseEntity<Void> confirmSchoolVerification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid SchoolVerificationConfirmRequest request) {
        schoolVerificationService.confirmVerification(userDetails.getUserId(), request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "매칭 신청", description = "로그인한 유학생이 같은 학교의 서포터즈 중 시간대가 겹치는 후보를 찾아 점수가 가장 높은 서포터즈와 매칭합니다.")
    @PostMapping
    public ResponseEntity<MatchResponse> applyMatch(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(matchingService.applyMatch(userDetails.getUserId()));
    }

    @Operation(summary = "매칭 결과 조회", description = "로그인한 사용자의 현재 MATCHED 상태 매칭 정보를 상대방 정보와 함께 조회합니다.")
    @GetMapping("/result")
    public ResponseEntity<MatchResponse> getMatchResult(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(matchingService.getMatchResult(userDetails.getUserId()));
    }

    @Operation(summary = "재매칭", description = "유학생 또는 서포터즈가 현재 매칭에 대해 재매칭을 요청합니다. 기존 매칭은 취소되고, 새로운 상대와 매칭됩니다. 사용자당 최대 3회까지 가능합니다.")
    @PostMapping("/{matchId}/rematch")
    public ResponseEntity<MatchResponse> rematch(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long matchId,
            @RequestBody(required = false) RematchRequest request) {
        return ResponseEntity.ok(matchingService.rematch(userDetails.getUserId(), matchId, request));
    }
}