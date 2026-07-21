package com.example.vivizip.document.service;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.document.entity.AnalysisStatus;
import com.example.vivizip.document.entity.ContractStage;
import com.example.vivizip.document.entity.LeaseCase;
import com.example.vivizip.document.entity.LeaseCaseStatus;
import com.example.vivizip.document.entity.LeaseDocumentType;
import com.example.vivizip.document.repository.DocumentAnalysisRepository;
import com.example.vivizip.document.repository.LeaseCaseRepository;
import com.example.vivizip.document.repository.LeaseDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 서류 분석이 완료될 때마다 호출되어, 임대차 케이스의 계약 단계(ContractStage)를 다시 계산·갱신한다.
// 임대차계약서(LEASE_CONTRACT)는 document_analysis에 결과를 저장하지 않는 1회성 분석이라
// 완료 여부를 LeaseCase.status(COMPLETED)로 판단한다 — LeaseContractAnalysisService가
// 분석 성공 시 이미 leaseCase.complete()를 호출해두기 때문에 별도 컬럼 없이 재사용 가능하다.
@Service
@RequiredArgsConstructor
public class ContractStageService {

    private final LeaseCaseRepository leaseCaseRepository;
    private final LeaseDocumentRepository leaseDocumentRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;

    @Transactional
    public void refreshStage(Long leaseCaseId) {
        LeaseCase leaseCase = leaseCaseRepository.findById(leaseCaseId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LEASE_CASE_NOT_FOUND));
        leaseCase.advanceContractStage(computeStage(leaseCase));
    }

    ContractStage computeStage(LeaseCase leaseCase) {
        Long leaseCaseId = leaseCase.getId();
        boolean duringReady = isAnalysisCompleted(leaseCaseId, LeaseDocumentType.REGISTRY)
                && isAnalysisCompleted(leaseCaseId, LeaseDocumentType.BUILDING_LEDGER);
        boolean afterReady = isAnalysisCompleted(leaseCaseId, LeaseDocumentType.BROKERAGE_CONFIRMATION)
                && leaseCase.getStatus() == LeaseCaseStatus.COMPLETED;

        if (afterReady) {
            return ContractStage.AFTER_CONTRACT;
        }
        if (duringReady) {
            return ContractStage.DURING_CONTRACT;
        }
        return ContractStage.BEFORE_CONTRACT;
    }

    private boolean isAnalysisCompleted(Long leaseCaseId, LeaseDocumentType documentType) {
        return leaseDocumentRepository.findFirstByLeaseCaseIdAndDocumentTypeOrderByIdDesc(leaseCaseId, documentType)
                .flatMap(document -> documentAnalysisRepository.findFirstByDocumentIdOrderByIdDesc(document.getId()))
                .map(analysis -> analysis.getStatus() == AnalysisStatus.COMPLETED)
                .orElse(false);
    }
}
