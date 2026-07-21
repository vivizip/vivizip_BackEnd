package com.example.vivizip.document.repository;

import com.example.vivizip.document.entity.LeaseDocument;
import com.example.vivizip.document.entity.LeaseDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeaseDocumentRepository extends JpaRepository<LeaseDocument, Long> {
    Optional<LeaseDocument> findByLeaseCaseIdAndDocumentType(Long leaseCaseId, LeaseDocumentType documentType);

    // 같은 케이스에 같은 타입 서류가 재업로드되어 여러 건 쌓여도 가장 최근 것을 조회
    Optional<LeaseDocument> findFirstByLeaseCaseIdAndDocumentTypeOrderByIdDesc(Long leaseCaseId, LeaseDocumentType documentType);
}
