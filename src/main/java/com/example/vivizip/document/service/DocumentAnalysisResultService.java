package com.example.vivizip.document.service;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.document.dto.BuildingLedgerAnalysisResponse;
import com.example.vivizip.document.dto.BuildingLedgerAnalysisResult;
import com.example.vivizip.document.dto.중개대상물.BrokerageDocumentAnalysisResponse;
import com.example.vivizip.document.entity.AnalysisStatus;
import com.example.vivizip.document.entity.DocumentAnalysis;
import com.example.vivizip.document.entity.LeaseDocument;
import com.example.vivizip.document.entity.LeaseDocumentType;
import com.example.vivizip.document.repository.DocumentAnalysisRepository;
import com.example.vivizip.document.repository.LeaseCaseRepository;
import com.example.vivizip.document.repository.LeaseDocumentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 건축물대장/중개대상물 확인·설명서는 업로드+분석 시 document_analysis에 결과(JSON)를 저장해두므로,
// 여기서는 그 저장된 결과를 다시 읽어 각 서류의 upload-analyze API와 동일한 응답 DTO로 돌려준다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentAnalysisResultService {

    private final LeaseCaseRepository leaseCaseRepository;
    private final LeaseDocumentRepository leaseDocumentRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final ObjectMapper objectMapper;

    public BuildingLedgerAnalysisResponse getBuildingLedgerResult(Long userId, Long leaseCaseId) {
        DocumentAnalysis analysis = findByLeaseCase(userId, leaseCaseId, LeaseDocumentType.BUILDING_LEDGER);
        BuildingLedgerAnalysisResult result = analysis.getResultJson() == null
                ? null
                : readJson(analysis.getResultJson(), BuildingLedgerAnalysisResult.class);
        return new BuildingLedgerAnalysisResponse(analysis.getId(), analysis.getStatus(), result, analysis.getFailureReason());
    }

    // BrokerageDocumentAnalysisResponse는 원래 API 응답 그대로라 status/failureReason을 담을 자리가 없어,
    // 완료된 결과가 없으면(PENDING/PROCESSING/FAILED) 조회 자체를 예외로 처리한다.
    public BrokerageDocumentAnalysisResponse getBrokerageDocumentResult(Long userId, Long leaseCaseId) {
        DocumentAnalysis analysis = findByLeaseCase(userId, leaseCaseId, LeaseDocumentType.BROKERAGE_CONFIRMATION);
        if (analysis.getStatus() != AnalysisStatus.COMPLETED || analysis.getResultJson() == null) {
            throw new GeneralException(ErrorStatus.DOCUMENT_ANALYSIS_RESULT_NOT_FOUND);
        }
        return readJson(analysis.getResultJson(), BrokerageDocumentAnalysisResponse.class);
    }

    private DocumentAnalysis findByLeaseCase(Long userId, Long leaseCaseId, LeaseDocumentType documentType) {
        leaseCaseRepository.findByIdAndUserId(leaseCaseId, userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LEASE_CASE_NOT_FOUND));

        LeaseDocument document = leaseDocumentRepository
                .findFirstByLeaseCaseIdAndDocumentTypeOrderByIdDesc(leaseCaseId, documentType)
                .orElseThrow(() -> new GeneralException(ErrorStatus.DOCUMENT_NOT_FOUND));

        return documentAnalysisRepository.findFirstByDocumentIdOrderByIdDesc(document.getId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.DOCUMENT_ANALYSIS_RESULT_NOT_FOUND));
    }

    private <T> T readJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }
}
