package com.example.vivizip.user.dto;

import com.example.vivizip.user.entity.Gender;
import com.example.vivizip.user.entity.Language;
import com.example.vivizip.user.entity.Nationality;
import com.example.vivizip.user.entity.User;

public record UserProfileResponse(
        Long id,
        String email,
        String nickname,
        String profileImage,
        String role,
        Language language,
        Nationality nationality,
        Gender gender,
        Long schoolId,
        Boolean schoolVerified
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getProfileImage(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getLanguage(),
                user.getNationality(),
                user.getGender(),
                user.getSchoolId(),
                user.getSchoolVerified()
        );
    }
}