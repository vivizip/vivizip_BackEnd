package com.example.vivizip.user.controller;

import com.example.vivizip.security.user.CustomUserDetails;
import com.example.vivizip.user.dto.ProfileImageResponse;
import com.example.vivizip.user.dto.UpdateLanguageRequest;
import com.example.vivizip.user.dto.UpdateProfileRequest;
import com.example.vivizip.user.dto.UserProfileResponse;
import com.example.vivizip.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static com.example.vivizip.consts.StaticVariable.SWAGGER_JWT;

@Tag(name = "User", description = "사용자 프로필 API")
@SecurityRequirement(name = SWAGGER_JWT)
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 프로필 조회")
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(userService.getProfile(userDetails.getEmail()));
    }

    @Operation(
            summary = "프로필 이미지 변경",
            description = "multipart/form-data로 이미지 파일을 업로드하면 S3에 저장 후 변경된 이미지 URL을 반환합니다.\n\n" +
                    "- 허용 형식: JPEG, PNG, WEBP\n" +
                    "- 최대 크기: 10MB\n" +
                    "- form-data key: `file`",
            requestBody = @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
    )
    @PatchMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileImageResponse updateProfileImage(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestPart("file") MultipartFile file) {
        String imageUrl = userService.updateProfileImage(user.getUserId(), file);
        return new ProfileImageResponse(imageUrl);
    }
    @Operation(summary = "언어 설정 변경", description = "앱 언어 설정 변경 (바텀시트)")
    @PatchMapping("/me/language")
    public ResponseEntity<UserProfileResponse> updateMyLanguage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UpdateLanguageRequest request) {
        return ResponseEntity.ok(userService.updateLanguage(userDetails.getEmail(), request));
    }

    @Operation(summary = "회원탈퇴", description = "status를 WITHDRAWN으로 변경 (소프트 삭제)")
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdrawMyAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.withdraw(userDetails.getEmail());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "회원탈퇴(완전 삭제)",
            description = "users row와 연관 데이터를 DB에서 완전히 삭제합니다. **되돌릴 수 없습니다.**\n\n" +
                    "- 본인 소유 데이터(임대차 케이스·서류·분석 결과, 활동 시간대, 알림, FCM 토큰, OCR 결과, 입주 기록/사진)는 모두 삭제됩니다.\n" +
                    "- 매칭·채팅방·채팅 메시지·약속은 상대방과 공유하는 데이터라도 함께 완전히 삭제됩니다(상대방 채팅 기록도 사라짐에 유의).\n" +
                    "- 삭제 후 같은 카카오 계정으로 다시 로그인하면 완전히 새 계정으로 가입됩니다."
    )
    @DeleteMapping("/me/hard")
    public ResponseEntity<Void> hardDeleteMyAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.hardDelete(userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }
}