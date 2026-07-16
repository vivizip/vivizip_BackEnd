package com.example.vivizip.document.service;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.document.entity.LeaseDocument;
import com.example.vivizip.document.entity.LeaseDocumentFile;
import com.example.vivizip.document.entity.LeaseDocumentType;
import com.example.vivizip.document.repository.LeaseCaseRepository;
import com.example.vivizip.document.repository.LeaseDocumentFileRepository;
import com.example.vivizip.document.repository.LeaseDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// LeaseDocument/LeaseDocumentFile 저장을 짧은 트랜잭션으로 커밋해, S3 업로드(외부 I/O)가
// 트랜잭션을 오래 붙잡지 않도록 서비스에서 분리했다. DocumentAnalysisRecorder와 같은 의도.
@Component
@RequiredArgsConstructor
class LeaseDocumentUploadRecorder {

    private final LeaseCaseRepository leaseCaseRepository;
    private final LeaseDocumentRepository leaseDocumentRepository;
    private final LeaseDocumentFileRepository leaseDocumentFileRepository;

    @Transactional(readOnly = true)
    public void assertOwnedLeaseCase(Long userId, Long leaseCaseId) {
        leaseCaseRepository.findByIdAndUserId(leaseCaseId, userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LEASE_CASE_NOT_FOUND));
    }

    @Transactional
    public LeaseDocument saveUploadedDocument(Long leaseCaseId, LeaseDocumentType documentType, List<String> s3Keys) {
        LeaseDocument document = leaseDocumentRepository.save(LeaseDocument.create(leaseCaseId, documentType));
        for (int i = 0; i < s3Keys.size(); i++) {
            leaseDocumentFileRepository.save(LeaseDocumentFile.create(document.getId(), s3Keys.get(i), i));
        }
        document.upload();
        return document;
    }
}
