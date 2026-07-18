package com.example.vivizip.matching.service;

import com.example.vivizip.matching.dto.MatchResponse;
import com.example.vivizip.matching.dto.MatchStatusResponse;
import com.example.vivizip.matching.dto.RematchRequest;
import com.example.vivizip.matching.dto.StudentOnboardingRequest;
import com.example.vivizip.matching.dto.SupporterOnboardingRequest;

public interface MatchingService {
    void onboardSupporter(Long userId, SupporterOnboardingRequest request);

    void onboardStudent(Long userId, StudentOnboardingRequest request);

    MatchResponse applyMatch(Long studentUserId);

    MatchResponse getMatchResult(Long userId);

    MatchResponse rematch(Long userId, Long matchId, RematchRequest request);

    MatchStatusResponse getMatchStatus(Long userId);
}