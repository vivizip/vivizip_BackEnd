package com.example.vivizip.document.service;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.document.dto.RegistryAnalysisResponse;
import com.example.vivizip.document.dto.RegistryAnalysisResult;
import com.example.vivizip.document.dto.RiskExplanation;
import com.example.vivizip.document.entity.AnalysisType;
import com.example.vivizip.document.entity.LeaseCase;
import com.example.vivizip.document.entity.LeaseDocument;
import com.example.vivizip.document.entity.LeaseDocumentType;
import com.example.vivizip.document.pipeline.RegistryReviewPipeline;
import com.example.vivizip.document.pipeline.RiskExplanationPipeline;
import com.example.vivizip.document.repository.LeaseCaseRepository;
import com.example.vivizip.document.repository.LeaseDocumentRepository;
import com.example.vivizip.user.entity.Nationality;
import com.example.vivizip.user.entity.User;
import com.example.vivizip.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistryAnalysisService {

    private final LeaseDocumentRepository leaseDocumentRepository;
    private final LeaseCaseRepository leaseCaseRepository;
    private final UserRepository userRepository;
    private final RegistryReviewPipeline registryReviewPipeline;
    private final RiskExplanationPipeline riskExplanationPipeline;
    private final DocumentAnalysisRecorder recorder;
    private final ObjectMapper objectMapper;

    // [테스트] documentId 직접 지정
    public RegistryAnalysisResponse analyze(Long userId, Long documentId, String ocrText) {
        LeaseDocument document = leaseDocumentRepository.findById(documentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.DOCUMENT_NOT_FOUND));
        if (document.getDocumentType() != LeaseDocumentType.REGISTRY) {
            throw new GeneralException(ErrorStatus.REGISTRY_DOCUMENT_TYPE_MISMATCH);
        }
        LeaseCase leaseCase = leaseCaseRepository.findById(document.getLeaseCaseId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.DOCUMENT_NOT_FOUND));
        if (!leaseCase.getUserId().equals(userId)) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }
        Nationality nationality = resolveNationality(userId);
        return analyzeInternal(documentId, leaseCase.getId(), ocrText, nationality);
    }

    // leaseCaseId로 document 자동 생성/재사용
    public RegistryAnalysisResponse analyzeByLeaseCaseId(Long userId, Long leaseCaseId, String ocrText) {
        LeaseCase leaseCase = leaseCaseRepository.findById(leaseCaseId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.DOCUMENT_NOT_FOUND));
        if (!leaseCase.getUserId().equals(userId)) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }
        LeaseDocument document = leaseDocumentRepository
                .findByLeaseCaseIdAndDocumentType(leaseCaseId, LeaseDocumentType.REGISTRY)
                .orElseGet(() -> leaseDocumentRepository.save(
                        LeaseDocument.createUploaded(leaseCaseId, LeaseDocumentType.REGISTRY)));
        Nationality nationality = resolveNationality(userId);
        return analyzeInternal(document.getId(), leaseCaseId, ocrText, nationality);
    }

    private RegistryAnalysisResponse analyzeInternal(Long documentId, Long leaseCaseId, String ocrText, Nationality nationality) {
        Long analysisId = recorder.start(documentId, AnalysisType.REGISTRY_ANALYSIS);
        RegistryAnalysisResult result;
        try {
            result = registryReviewPipeline.analyze(ocrText);
        } catch (GeneralException e) {
            recorder.fail(documentId, analysisId, e.getMessage());
            throw e;
        }
        recorder.complete(documentId, analysisId, AnalysisType.REGISTRY_ANALYSIS, result, toJson(result));
        recorder.saveRegistryBaseline(leaseCaseId, result);

        List<RiskExplanation> riskExplanations = riskExplanationPipeline.explain(
                result.riskFlags().detected(), nationality);
        return new RegistryAnalysisResponse(result, riskExplanations);
    }

    private Nationality resolveNationality(Long userId) {
        return userRepository.findById(userId)
                .map(User::getNationality)
                .orElse(null);
    }

    private String toJson(RegistryAnalysisResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }
}
