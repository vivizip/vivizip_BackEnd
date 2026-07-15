package com.example.vivizip.document.service;

import com.example.vivizip.document.dto.AnalysisResult;
import com.example.vivizip.document.dto.DocumentAnalysisResponse;
import com.example.vivizip.document.entity.AnalysisType;
import com.example.vivizip.document.entity.DocumentAnalysis;
import com.example.vivizip.document.entity.LeaseDocument;
import com.example.vivizip.document.repository.DocumentAnalysisRepository;
import com.example.vivizip.document.repository.LeaseDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// document_analysis/lease_document 상태 전이를 각각 독립 트랜잭션으로 커밋한다.
// DocumentAnalysisService.analyze()가 pipeline(외부 LLM 호출) 도중 예외를 다시 던져도
// 이미 커밋된 start()/fail() 상태가 같은 트랜잭션 롤백에 휩쓸리지 않도록 별도 빈으로 분리했다.
@Component
@RequiredArgsConstructor
class DocumentAnalysisRecorder {

    private final LeaseDocumentRepository leaseDocumentRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;

    @Transactional
    public Long start(Long documentId, AnalysisType analysisType) {
        LeaseDocument document = leaseDocumentRepository.getReferenceById(documentId);
        document.startAnalyzing();

        DocumentAnalysis analysis = DocumentAnalysis.create(documentId, analysisType);
        analysis.start();
        return documentAnalysisRepository.save(analysis).getId();
    }

    @Transactional
    public DocumentAnalysisResponse complete(Long documentId, Long analysisId, AnalysisType analysisType,
                                              AnalysisResult result, String resultJson) {
        DocumentAnalysis analysis = documentAnalysisRepository.getReferenceById(analysisId);
        analysis.complete(resultJson, null);

        LeaseDocument document = leaseDocumentRepository.getReferenceById(documentId);
        document.completeAnalysis();

        return new DocumentAnalysisResponse(analysisId, analysisType, analysis.getStatus(), result, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long documentId, Long analysisId, String reason) {
        documentAnalysisRepository.findById(analysisId).ifPresent(analysis -> analysis.fail(reason));
        leaseDocumentRepository.findById(documentId).ifPresent(document -> document.failAnalysis(reason));
    }
}