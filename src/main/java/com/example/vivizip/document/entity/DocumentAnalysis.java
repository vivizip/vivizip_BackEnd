package com.example.vivizip.document.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "document_analysis")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_type", nullable = false, length = 50)
    private AnalysisType analysisType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AnalysisStatus status;

    // 구조화된 전체 분석 결과 (서류별 스키마 상이)
    @Column(name = "result_json", columnDefinition = "json")
    private String resultJson;

    // AI 원본 응답 (디버깅용)
    @Column(name = "raw_response_json", columnDefinition = "json")
    private String rawResponseJson;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    private DocumentAnalysis(Long documentId, AnalysisType analysisType) {
        this.documentId = documentId;
        this.analysisType = analysisType;
        this.status = AnalysisStatus.PENDING;
    }

    public static DocumentAnalysis create(Long documentId, AnalysisType analysisType) {
        return new DocumentAnalysis(documentId, analysisType);
    }

    public void start() {
        if (status != AnalysisStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 분석을 시작할 수 있습니다. 현재 상태: " + status);
        }
        this.status = AnalysisStatus.PROCESSING;
        this.startedAt = LocalDateTime.now();
    }

    public void complete(String resultJson, String rawResponseJson) {
        if (status != AnalysisStatus.PROCESSING) {
            throw new IllegalStateException("PROCESSING 상태에서만 분석을 완료할 수 있습니다. 현재 상태: " + status);
        }
        this.failureReason = null;
        this.resultJson = resultJson;
        this.rawResponseJson = rawResponseJson;
        this.status = AnalysisStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String failureReason) {
        if (status != AnalysisStatus.PROCESSING) {
            throw new IllegalStateException("PROCESSING 상태에서만 분석 실패 처리를 할 수 있습니다. 현재 상태: " + status);
        }
        this.resultJson = null;
        this.rawResponseJson = null;
        this.failureReason = failureReason;
        this.status = AnalysisStatus.FAILED;
        this.completedAt = LocalDateTime.now();
    }
}