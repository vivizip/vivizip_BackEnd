package com.example.vivizip.ocr.repository;

import com.example.vivizip.ocr.entity.OcrResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OcrResultRepository extends JpaRepository<OcrResult, Long> {
    void deleteByUserId(Long userId);
}
