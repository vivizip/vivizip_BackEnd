package com.example.vivizip.matching.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SchoolVerificationSendRequest(
        @NotBlank(message = "schoolEmail은 필수입니다.")
        @Email(message = "schoolEmail 형식이 올바르지 않습니다.")
        String schoolEmail
) {}