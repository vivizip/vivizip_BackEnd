package com.example.vivizip.document.repository;

import com.example.vivizip.document.entity.DocumentAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentAnalysisRepository extends JpaRepository<DocumentAnalysis, Long> {
    Optional<DocumentAnalysis> findFirstByDocumentIdOrderByIdDesc(Long documentId);
}