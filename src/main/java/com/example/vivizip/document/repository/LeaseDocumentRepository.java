package com.example.vivizip.document.repository;

import com.example.vivizip.document.entity.LeaseDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaseDocumentRepository extends JpaRepository<LeaseDocument, Long> {
}
