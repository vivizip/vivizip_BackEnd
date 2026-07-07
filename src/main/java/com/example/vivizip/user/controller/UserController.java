package com.example.vivizip.user.controller;

import com.example.vivizip.user.dto.UpdateLanguageRequest;
import com.example.vivizip.user.dto.UpdateProfileRequest;
import com.example.vivizip.user.dto.UserProfileResponse;
import com.example.vivizip.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 프로필 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 프로필 조회")
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(userService.getProfile(authentication.getName()));
    }

    @Operation(summary = "프로필 수정", description = "nickname, profileImage 수정 가능. 학교 정보는 수정 불가")
    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(userService.updateProfile(authentication.getName(), request));
    }

    @Operation(summary = "언어 설정 변경", description = "앱 언어 설정 변경 (바텀시트)")
    @PatchMapping("/me/language")
    public ResponseEntity<UserProfileResponse> updateMyLanguage(
            Authentication authentication,
            @RequestBody @Valid UpdateLanguageRequest request
    ) {
        return ResponseEntity.ok(userService.updateLanguage(authentication.getName(), request));
    }

    @Operation(summary = "회원탈퇴", description = "status를 WITHDRAWN으로 변경 (소프트 삭제)")
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdrawMyAccount(Authentication authentication) {
        userService.withdraw(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
