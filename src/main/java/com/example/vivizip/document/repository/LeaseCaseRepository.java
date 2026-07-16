package com.example.vivizip.document.repository;

import com.example.vivizip.document.entity.LeaseCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaseCaseRepository extends JpaRepository<LeaseCase, Long> {
    List<LeaseCase> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<LeaseCase> findByIdAndUserId(Long id, Long userId);
}
