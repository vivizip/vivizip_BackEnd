package com.example.vivizip.notification.controller;

import com.example.vivizip.notification.dto.FcmTokenRequest;
import com.example.vivizip.notification.dto.NotificationSliceResponse;
import com.example.vivizip.notification.dto.UnreadCountResponse;
import com.example.vivizip.notification.service.NotificationService;
import com.example.vivizip.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Notification",
        description = """
                알림 API
                - Firebase 프로젝트는 solutionyesfuture@gmail.com 계정으로 만들었습니다! 토큰 받으면 로그인 후 등록,로그아웃 시 삭제하면 됩니다!
                """
)
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "알림 목록 조회", description = "커서 기반 페이징으로 최신순 알림 목록을 반환합니다. cursor가 없으면 최신 알림부터 반환합니다.")
    @GetMapping
    public NotificationSliceResponse getNotifications(
            @AuthenticationPrincipal CustomUserDetails user,
            @Parameter(description = "마지막으로 받은 알림 ID (첫 요청 시 생략)") @RequestParam(required = false) Long cursor,
            @Parameter(description = "한 번에 조회할 알림 수 (기본값: 20)") @RequestParam(defaultValue = "20") int size) {
        return notificationService.getMyNotifications(user.getUserId(), cursor, size);
    }

    @Operation(summary = "읽지 않은 알림 개수 조회")
    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount(@AuthenticationPrincipal CustomUserDetails user) {
        return notificationService.getUnreadCount(user.getUserId());
    }

    @Operation(summary = "모든 알림 읽음 처리", description = "안 읽은 알림을 전부 읽음 처리합니다. 응답의 unreadCount는 항상 0입니다.")
    @PatchMapping("/read-all")
    public UnreadCountResponse readAll(@AuthenticationPrincipal CustomUserDetails user) {
        return notificationService.readAll(user.getUserId());
    }

    @Operation(summary = "개별 알림 읽음 처리")
    @PatchMapping("/{id}/read")
    public void readOne(
            @AuthenticationPrincipal CustomUserDetails user,
            @Parameter(description = "알림 ID") @PathVariable Long id) {
        notificationService.readOne(user.getUserId(), id);
    }

    @Operation(summary = "FCM 토큰 등록/갱신", description = "기기의 FCM 토큰을 등록합니다. 이미 등록된 토큰이면 소유자를 현재 로그인 사용자로 갱신합니다.")
    @PostMapping("/fcm-token")
    public void registerFcmToken(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid FcmTokenRequest request) {
        notificationService.registerFcmToken(user.getUserId(), request.token());
    }

    @Operation(summary = "FCM 토큰 삭제", description = "로그아웃 시 현재 기기의 FCM 토큰을 제거합니다.")
    @DeleteMapping("/fcm-token")
    public void deleteFcmToken(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid FcmTokenRequest request) {
        notificationService.deleteFcmToken(user.getUserId(), request.token());
    }

    @Operation(
            summary = "[테스트] 아티클 업로드 알림 발송",
            description = "실제 아티클 발행 기능이 아직 없어, 알림 발송 로직만 확인하기 위한 테스트용 엔드포인트입니다. " +
                    "ACTIVE 상태인 모든 유저에게 ARTICLE_UPLOADED 알림을 보냅니다."
    )
    @PostMapping("/test/article-uploaded")
    public void testArticleUploaded(
            @Parameter(description = "알림 제목") @RequestParam(defaultValue = "새로운 아티클이 업로드되었어요") String title,
            @Parameter(description = "알림 본문") @RequestParam(defaultValue = "비자 변경 날짜가 다가오지 않으셨나요? '확정날짜 받는 방법'을 알아보세요") String body,
            @Parameter(description = "탭 시 이동할 아티클 id (없어도 됨)") @RequestParam(required = false) Long articleId) {
        notificationService.notifyArticleUploaded(title, body, articleId);
    }
}