package com.example.vivizip.document.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "lease_document")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeaseDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lease_case_id", nullable = false)
    private Long leaseCaseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private LeaseDocumentType documentType;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LeaseDocumentStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    private LeaseDocument(Long leaseCaseId, LeaseDocumentType documentType) {
        this.leaseCaseId = leaseCaseId;
        this.documentType = documentType;
        this.status = LeaseDocumentStatus.ISSUING;
    }

    public static LeaseDocument create(Long leaseCaseId, LeaseDocumentType documentType) {
        return new LeaseDocument(leaseCaseId, documentType);
    }

    public void upload(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalArgumentException("fileUrl은 비어 있을 수 없습니다.");
        }
        if (status != LeaseDocumentStatus.ISSUING) {
            throw new IllegalStateException("ISSUING 상태에서만 업로드할 수 있습니다. 현재 상태: " + status);
        }
        this.fileUrl = fileUrl;
        this.status = LeaseDocumentStatus.UPLOADED;
    }

    public void startAnalyzing() {
        if (status != LeaseDocumentStatus.UPLOADED
                && status != LeaseDocumentStatus.ANALYZED
                && status != LeaseDocumentStatus.ANALYSIS_FAILED) {
            throw new IllegalStateException("UPLOADED/ANALYZED/ANALYSIS_FAILED 상태에서만 분석을 시작할 수 있습니다. 현재 상태: " + status);
        }
        this.failureReason = null;
        this.status = LeaseDocumentStatus.ANALYZING;
    }

    public void completeAnalysis() {
        if (status != LeaseDocumentStatus.ANALYZING) {
            throw new IllegalStateException("ANALYZING 상태에서만 분석을 완료할 수 있습니다. 현재 상태: " + status);
        }
        this.failureReason = null;
        this.status = LeaseDocumentStatus.ANALYZED;
    }

    public void failAnalysis(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            throw new IllegalArgumentException("failureReason은 비어 있을 수 없습니다.");
        }
        if (status != LeaseDocumentStatus.ANALYZING) {
            throw new IllegalStateException("ANALYZING 상태에서만 분석 실패 처리를 할 수 있습니다. 현재 상태: " + status);
        }
        this.failureReason = failureReason;
        this.status = LeaseDocumentStatus.ANALYSIS_FAILED;
    }
}