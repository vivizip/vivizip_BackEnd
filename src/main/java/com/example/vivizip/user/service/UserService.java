package com.example.vivizip.user.service;

import com.example.vivizip.user.dto.UpdateLanguageRequest;
import com.example.vivizip.user.dto.UpdateProfileRequest;
import com.example.vivizip.user.dto.UserProfileResponse;

public interface UserService {
    UserProfileResponse getProfile(String email);
    UserProfileResponse updateProfile(String email, UpdateProfileRequest request);
    UserProfileResponse updateLanguage(String email, UpdateLanguageRequest request);
    void withdraw(String email);
}