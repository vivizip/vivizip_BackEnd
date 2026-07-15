package com.example.vivizip.document.repository;

import com.example.vivizip.document.entity.ReferenceBaseline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReferenceBaselineRepository extends JpaRepository<ReferenceBaseline, Long> {
    Optional<ReferenceBaseline> findByLeaseCaseId(Long leaseCaseId);
}
