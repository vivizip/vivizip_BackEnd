package com.example.vivizip.matching.repository;

import com.example.vivizip.matching.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolRepository extends JpaRepository<School, Long> {
}