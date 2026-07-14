package com.example.vivizip.document.entity;

import com.example.vivizip.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "document_review")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentReviewStatus status;

    // OCR/전처리를 거친 뒤 LLM에 실제로 전달된 텍스트
    @Column(name = "original_text", nullable = false, columnDefinition = "LONGTEXT")
    private String originalText;

    // LLM이 반환한 구조화 결과(AnalysisResult 구현체)를 JSON 그대로 보관 (감사/재처리용)
    @Column(name = "result_json", columnDefinition = "LONGTEXT")
    private String resultJson;

    // 목록/알림 등에서 빠르게 보여줄 요약
    @Column(length = 1000)
    private String summary;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    private DocumentReview(Long userId, DocumentType documentType, String originalText) {
        this.userId = userId;
        this.documentType = documentType;
        this.originalText = originalText;
        this.status = DocumentReviewStatus.PENDING;
    }

    public static DocumentReview pending(Long userId, DocumentType documentType, String originalText) {
        return new DocumentReview(userId, documentType, originalText);
    }

    public void complete(String resultJson, String summary) {
        this.resultJson = resultJson;
        this.summary = summary;
        this.status = DocumentReviewStatus.COMPLETED;
    }

    public void fail(String failureReason) {
        this.failureReason = failureReason;
        this.status = DocumentReviewStatus.FAILED;
    }
}
