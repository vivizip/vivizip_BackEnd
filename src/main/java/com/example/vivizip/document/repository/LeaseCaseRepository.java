package com.example.vivizip.document.repository;

import com.example.vivizip.document.entity.LeaseCase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaseCaseRepository extends JpaRepository<LeaseCase, Long> {
}
