package com.example.vivizip.ocr.dto;

import java.util.List;
import java.util.UUID;

public record ClovaOcrRequest(
        String version,
        String requestId,
        Long timestamp,
        String lang,                    // "ko" 추가
        Boolean enableTableDetection,   // true
        List<Image> images
) {
    public record Image(
            String format,   // "jpg", "png" 등
            String name,
            String data      // base64 인코딩된 이미지
    ) {}

    public static ClovaOcrRequest ofSingleImage(String format, String base64Data) {
        return new ClovaOcrRequest(
                "V2",
                java.util.UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                "ko",
                true,                   // 표 추출 켜기
                List.of(new Image(format, "test-doc", base64Data))
        );
    }
}
