package com.example.vivizip.ocr.dto;

import com.example.vivizip.ocr.entity.OcrResult;

import java.time.LocalDateTime;

public record OcrSaveResponse(Long id, int pageCount, LocalDateTime createdAt) {

    public static OcrSaveResponse of(OcrResult result, int pageCount) {
        return new OcrSaveResponse(result.getId(), pageCount, result.getCreatedAt());
    }
}
