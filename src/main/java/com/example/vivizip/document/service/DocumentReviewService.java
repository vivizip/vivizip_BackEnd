package com.example.vivizip.document.service;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.document.dto.DocumentReviewResponse;
import com.example.vivizip.document.dto.LeaseContractAnalysisResult;
import com.example.vivizip.document.entity.DocumentReview;
import com.example.vivizip.document.entity.DocumentType;
import com.example.vivizip.document.pipeline.DocumentAnalysisPipeline;
import com.example.vivizip.document.repository.DocumentReviewRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentReviewService {

    private final DocumentAnalysisPipeline<LeaseContractAnalysisResult> leaseContractReviewPipeline;
    private final DocumentReviewRepository documentReviewRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public DocumentReviewResponse reviewLeaseContract(Long userId, String documentText) {
        DocumentReview review = documentReviewRepository.save(
                DocumentReview.pending(userId, DocumentType.LEASE_CONTRACT, documentText));

        LeaseContractAnalysisResult result;
        try {
            result = leaseContractReviewPipeline.analyze(documentText);
        } catch (GeneralException e) {
            review.fail(e.getMessage());
            throw e;
        }

        review.complete(toJson(result), result.summary());
        return DocumentReviewResponse.of(review, result);
    }

    private String toJson(LeaseContractAnalysisResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }
}
