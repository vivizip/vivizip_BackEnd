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
import com.example.vivizip.document.entity.ReferenceBaseline;
import com.example.vivizip.document.pipeline.RegistryReviewPipeline;
import com.example.vivizip.document.pipeline.RiskExplanationPipeline;
import com.example.vivizip.document.repository.LeaseCaseRepository;
import com.example.vivizip.document.repository.LeaseDocumentRepository;
import com.example.vivizip.document.repository.ReferenceBaselineRepository;
import com.example.vivizip.housingprice.dto.HousingPriceSearchResult;
import com.example.vivizip.housingprice.service.HousingPriceSearchService;
import com.example.vivizip.user.entity.Nationality;
import com.example.vivizip.user.entity.User;
import com.example.vivizip.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistryAnalysisService {

    private final LeaseDocumentRepository leaseDocumentRepository;
    private final LeaseCaseRepository leaseCaseRepository;
    private final ReferenceBaselineRepository referenceBaselineRepository;
    private final UserRepository userRepository;
    private final RegistryReviewPipeline registryReviewPipeline;
    private final RiskExplanationPipeline riskExplanationPipeline;
    private final HousingPriceSearchService housingPriceSearchService;
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

        ReferenceBaseline baseline = referenceBaselineRepository.findByLeaseCaseId(leaseCaseId).orElse(null);
        Long marketPrice = resolveMarketPrice(leaseCaseId);
        Double depositRiskRatio = calculateDepositRiskRatio(result.mortgageMaximumClaimAmount(), baseline, marketPrice);

        return new RegistryAnalysisResponse(result, riskExplanations, marketPrice, depositRiskRatio);
    }

    // 사용자가 계약 건을 만들 때 직접 입력한 LeaseCase.roadAddress로 공시가격을 조회한다.
    // reference_baseline과 달리 등기부/중개대상물 분석 여부와 무관하게 항상 존재한다.
    // juso 호출 실패 등으로 시세를 못 구해도 등기부 분석 자체는 실패시키지 않고 null로 둔다.
    private Long resolveMarketPrice(Long leaseCaseId) {
        LeaseCase leaseCase = leaseCaseRepository.findById(leaseCaseId).orElse(null);
        if (leaseCase == null || leaseCase.getRoadAddress() == null) {
            return null;
        }
        try {
            HousingPriceSearchResult priceResult = housingPriceSearchService.searchByRoadAddress(leaseCase.getRoadAddress());
            return priceResult == null ? null : priceResult.price();
        } catch (RuntimeException e) {
            log.warn("[Registry] 시세 조회 실패, marketPrice=null 처리: leaseCaseId={}, reason={}", leaseCaseId, e.getMessage());
            return null;
        }
    }

    // ((채권최고액 + 내 보증금) / 시세) * 100. 채권최고액이 없으면(근저당 없음) 0으로 취급하고,
    // 보증금 또는 시세를 구하지 못했으면 계산 자체를 하지 않고 null을 반환한다.
    private Double calculateDepositRiskRatio(Long mortgageMaximumClaimAmount, ReferenceBaseline baseline, Long marketPrice) {
        if (baseline == null || baseline.getDeposit() == null || marketPrice == null || marketPrice == 0) {
            return null;
        }
        long claim = mortgageMaximumClaimAmount != null ? mortgageMaximumClaimAmount : 0L;
        double ratio = ((double) (claim + baseline.getDeposit()) / marketPrice) * 100;
        return Math.round(ratio * 100.0) / 100.0;
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
