package com.example.vivizip.document.repository;

import com.example.vivizip.document.entity.LeaseDocumentFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaseDocumentFileRepository extends JpaRepository<LeaseDocumentFile, Long> {
}
