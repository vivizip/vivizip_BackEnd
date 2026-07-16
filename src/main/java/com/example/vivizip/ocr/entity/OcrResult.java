package com.example.vivizip.ocr.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * CLOVA OCR 결과 저장
 * - rawJson: List<ClovaOcrResponse> 직렬화 전체 (LONGTEXT)
 * - 검색은 애플리케이션에서 역직렬화 후 수행
 */
@Entity
@Table(name = "ocr_result")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class OcrResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String rawJson;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public static OcrResult create(Long userId, String rawJson) {
        OcrResult o = new OcrResult();
        o.userId = userId;
        o.rawJson = rawJson;
        return o;
    }
}
