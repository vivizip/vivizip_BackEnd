package com.example.vivizip.matching.service;

import com.example.vivizip.matching.dto.SchoolVerificationConfirmRequest;
import com.example.vivizip.matching.dto.SchoolVerificationSendRequest;

public interface SchoolVerificationService {
    void sendVerification(Long userId, SchoolVerificationSendRequest request);

    void confirmVerification(Long userId, SchoolVerificationConfirmRequest request);
}