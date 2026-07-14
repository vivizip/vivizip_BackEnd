package com.example.vivizip.document.dto;

import com.example.vivizip.document.entity.DocumentReview;
import com.example.vivizip.document.entity.DocumentReviewStatus;

import java.time.LocalDateTime;
import java.util.List;

public record DocumentReviewResponse(
        Long id,
        DocumentReviewStatus status,
        String summary,
        List<LeaseContractAnalysisResult.RiskClause> riskClauses,
        List<String> checklist,
        LocalDateTime createdAt
) {
    public static DocumentReviewResponse of(DocumentReview review, LeaseContractAnalysisResult result) {
        return new DocumentReviewResponse(
                review.getId(),
                review.getStatus(),
                result.summary(),
                result.riskClauses(),
                result.checklist(),
                review.getCreatedAt()
        );
    }
}
