package com.example.vivizip.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserResponse(
        Long id,

        @JsonProperty("kakao_account")
        KakaoAccount kakaoAccount,

        Properties properties
) {
    public record KakaoAccount(
            String email,
            Profile profile
    ) {
    }

    public record Profile(
            String nickname,

            @JsonProperty("profile_image_url")
            String profileImageUrl
    ) {
    }

    public record Properties(
            String nickname
    ) {
    }

    public String getEmail() {
        if (kakaoAccount == null) return null;
        return kakaoAccount.email();
    }

    public String getNickname() {
        if (kakaoAccount != null && kakaoAccount.profile() != null) {
            return kakaoAccount.profile().nickname();
        }

        if (properties != null) {
            return properties.nickname();
        }

        return "카카오사용자";
    }

    public String getProfileImageUrl() {
        if (kakaoAccount == null || kakaoAccount.profile() == null) {
            return null;
        }

        return kakaoAccount.profile().profileImageUrl();
    }
}