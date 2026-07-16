package com.example.vivizip.document.repository;

import com.example.vivizip.document.entity.LeaseDocument;
import com.example.vivizip.document.entity.LeaseDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeaseDocumentRepository extends JpaRepository<LeaseDocument, Long> {
    Optional<LeaseDocument> findByLeaseCaseIdAndDocumentType(Long leaseCaseId, LeaseDocumentType documentType);
}
