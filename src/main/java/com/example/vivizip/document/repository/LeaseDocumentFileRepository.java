package com.example.vivizip.document.repository;

import com.example.vivizip.document.entity.LeaseDocumentFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaseDocumentFileRepository extends JpaRepository<LeaseDocumentFile, Long> {
    List<LeaseDocumentFile> findAllByLeaseDocumentId(Long leaseDocumentId);
}
