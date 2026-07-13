package com.example.vivizip.matching.repository;

import com.example.vivizip.matching.entity.SchoolEmailDomain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchoolEmailDomainRepository extends JpaRepository<SchoolEmailDomain, Long> {
    Optional<SchoolEmailDomain> findByEmailDomain(String emailDomain);
}