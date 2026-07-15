package com.example.vivizip.document.service;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.document.dto.AnalysisResult;
import com.example.vivizip.document.dto.BuildingLedgerAnalysisResult;
import com.example.vivizip.document.dto.DocumentAnalysisResponse;
import com.example.vivizip.document.entity.AnalysisType;
import com.example.vivizip.document.entity.LeaseCase;
import com.example.vivizip.document.entity.LeaseDocument;
import com.example.vivizip.document.pipeline.DocumentAnalysisPipeline;
import com.example.vivizip.document.repository.LeaseCaseRepository;
import com.example.vivizip.document.repository.LeaseDocumentRepository;
import com.example.vivizip.document.repository.ReferenceBaselineRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// OCR 텍스트를 받아 서류 타입에 맞는 pipeline을 실행하고, 결과를 document_analysis에 저장한다.
// 등기부와 대조가 필요한 서류(현재 건축물대장)는 reference_baseline과 비교해 일치 여부를 채운다.
@Service
@RequiredArgsConstructor
public class DocumentAnalysisService {

    private final LeaseCaseRepository leaseCaseRepository;
    private final LeaseDocumentRepository leaseDocumentRepository;
    private final ReferenceBaselineRepository referenceBaselineRepository;
    private final ObjectMapper objectMapper;
    private final List<DocumentAnalysisPipeline<? extends AnalysisResult>> pipelines;
    private final DocumentAnalysisRecorder recorder;

    private Map<AnalysisType, DocumentAnalysisPipeline<? extends AnalysisResult>> pipelinesByType;

    @PostConstruct
    void initPipelines() {
        pipelinesByType = pipelines.stream()
                .collect(Collectors.toMap(DocumentAnalysisPipeline::type, Function.identity()));
    }

    public DocumentAnalysisResponse analyze(Long userId, Long documentId, String ocrText) {
        LeaseDocument document = leaseDocumentRepository.findById(documentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.DOCUMENT_NOT_FOUND));
        LeaseCase leaseCase = leaseCaseRepository.findById(document.getLeaseCaseId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.DOCUMENT_NOT_FOUND));
        if (!leaseCase.getUserId().equals(userId)) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }

        AnalysisType analysisType = document.getDocumentType().analysisType();
        DocumentAnalysisPipeline<? extends AnalysisResult> pipeline = pipelinesByType.get(analysisType);
        if (pipeline == null) {
            throw new GeneralException(ErrorStatus.DOCUMENT_TYPE_NOT_SUPPORTED);
        }

        Long leaseCaseId = document.getLeaseCaseId();
        Long analysisId = recorder.start(documentId, analysisType);

        try {
            AnalysisResult result = pipeline.analyze(ocrText);
            result = applyReferenceCheck(leaseCaseId, analysisType, result);
            return recorder.complete(documentId, analysisId, analysisType, result, toJson(result));
        } catch (RuntimeException e) {
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            recorder.fail(documentId, analysisId, reason);
            throw e;
        }
    }

    private AnalysisResult applyReferenceCheck(Long leaseCaseId, AnalysisType analysisType, AnalysisResult result) {
        if (analysisType != AnalysisType.BUILDING_LEDGER_ANALYSIS || !(result instanceof BuildingLedgerAnalysisResult r)) {
            return result;
        }
        return referenceBaselineRepository.findByLeaseCaseId(leaseCaseId)
                .<AnalysisResult>map(baseline -> new BuildingLedgerAnalysisResult(
                        r.issuedDate(), r.hasViolation(), r.ownerName(), r.ownershipTransferDate(), r.address(),
                        r.buildingUse(), r.residential(),
                        matches(r.ownerName(), baseline.getOwnerName()),
                        matches(r.address(), baseline.getPropertyAddress())))
                .orElse(result);
    }

    private boolean matches(String a, String b) {
        return normalize(a).equals(normalize(b));
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("[\\s()]", "");
    }

    private String toJson(AnalysisResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }
}