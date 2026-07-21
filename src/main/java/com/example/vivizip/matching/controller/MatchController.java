package com.example.vivizip.matching.controller;

import com.example.vivizip.matching.dto.MatchResponse;
import com.example.vivizip.matching.dto.MatchStatusResponse;
import com.example.vivizip.matching.dto.RematchRequest;
import com.example.vivizip.matching.dto.SchoolVerificationConfirmRequest;
import com.example.vivizip.matching.dto.SchoolVerificationSendRequest;
import com.example.vivizip.matching.dto.StudentOnboardingRequest;
import com.example.vivizip.matching.dto.SupporterOnboardingRequest;
import com.example.vivizip.matching.dto.TimeSlotResponse;
import com.example.vivizip.matching.dto.UpdateTimeSlotsRequest;
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

import java.util.List;

import static com.example.vivizip.consts.StaticVariable.SWAGGER_JWT;

@Tag(name = "Match", description = "유학생-서포터즈 매칭 API")
@SecurityRequirement(name = SWAGGER_JWT)
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private static final String COUNTERPART_KOREAN_LEVEL_NOTE =
            "`counterpartKoreanLevel`은 상대가 서포터즈면 항상 `null`입니다 (서포터즈 온보딩에는 한국어 수준 항목이 없음).";

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

    @Operation(
            summary = "매칭 상태 조회",
            description = "로그인한 사용자의 매칭 진행 상태를 3단계로 반환합니다.\n\n" +
                    "- `NOT_APPLIED`: 신청 전. 온보딩(서포터즈/유학생 등록)을 아직 안 했거나, " +
                    "유학생이 매칭 신청 자체를 한 적 없거나, 매칭이 취소된 후 재신청하지 않은 경우\n" +
                    "- `APPLIED_NOT_MATCHED`: 신청 후 매칭 전(대기 중). 유학생은 매칭 신청(POST /api/matches)했지만 " +
                    "후보가 없어 PENDING으로 대기 중인 경우, 서포터즈는 온보딩만으로 후보 풀에 들어가 아직 선택되지 않은 경우\n" +
                    "- `MATCHED`: 매칭된 후. 현재 MATCHED 상태 매칭이 있음\n\n" +
                    "유학생 기준으로는 매칭(matches) 레코드의 실제 상태(PENDING/MATCHED)를 직접 확인하며, " +
                    "CANCELED만 있고 재신청하지 않았으면 NOT_APPLIED로 취급합니다."
    )
    @GetMapping("/status")
    public ResponseEntity<MatchStatusResponse> getMatchStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(matchingService.getMatchStatus(userDetails.getUserId()));
    }

    @Operation(
            summary = "매칭 신청",
            description = "로그인한 유학생이 같은 학교의 서포터즈 중 시간대가 겹치는 후보를 찾아 점수가 가장 높은 서포터즈와 매칭합니다.\n\n" +
                    "후보가 없으면 매칭 신청 자체는 PENDING 상태로 남겨두고(매칭 상태 조회 시 대기 중으로 보임) " +
                    "404(MATCH_CANDIDATE_NOT_FOUND)를 반환합니다. 이미 PENDING 상태로 대기 중이면 400(MATCH_ALREADY_PENDING)을 반환합니다.\n\n" +
                    COUNTERPART_KOREAN_LEVEL_NOTE
    )
    @PostMapping
    public ResponseEntity<MatchResponse> applyMatch(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(matchingService.applyMatch(userDetails.getUserId()));
    }

    @Operation(
            summary = "매칭 결과 조회",
            description = "로그인한 사용자의 현재 MATCHED 상태 매칭 정보를 상대방 정보와 함께 조회합니다.\n\n" +
                    COUNTERPART_KOREAN_LEVEL_NOTE
    )
    @GetMapping("/result")
    public ResponseEntity<MatchResponse> getMatchResult(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(matchingService.getMatchResult(userDetails.getUserId()));
    }

    @Operation(summary = "내 활동 시간대 조회", description = "로그인한 사용자가 등록한 활동 가능 시간대 목록을 조회합니다. (마이페이지)")
    @GetMapping("/time-slots")
    public ResponseEntity<List<TimeSlotResponse>> getTimeSlots(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(matchingService.getTimeSlots(userDetails.getUserId()));
    }

    @Operation(summary = "내 활동 시간대 수정", description = "등록된 시간대를 요청 목록으로 전체 교체합니다. (마이페이지)")
    @PutMapping("/time-slots")
    public ResponseEntity<Void> updateTimeSlots(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UpdateTimeSlotsRequest request) {
        matchingService.updateTimeSlots(userDetails.getUserId(), request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "재매칭",
            description = "유학생 또는 서포터즈가 현재 매칭에 대해 재매칭을 요청합니다. 기존 매칭은 취소되고, 새로운 상대와 매칭됩니다. " +
                    "사용자당 최대 3회까지 가능합니다.\n\n" +
                    COUNTERPART_KOREAN_LEVEL_NOTE
    )
    @PostMapping("/{matchId}/rematch")
    public ResponseEntity<MatchResponse> rematch(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long matchId,
            @RequestBody(required = false) RematchRequest request) {
        return ResponseEntity.ok(matchingService.rematch(userDetails.getUserId(), matchId, request));
    }
}