package com.example.vivizip.document.dto;

public record LeaseCaseProgressResponse(
        int currentStep,        // 1: 메이트 매칭, 2: 집 구하는 중, 3: 계약서 검토
        String currentStepLabel
) {}