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
        this.fileUrl = fileUrl;
        this.status = LeaseDocumentStatus.UPLOADED;
    }

    public void startAnalyzing() {
        this.status = LeaseDocumentStatus.ANALYZING;
    }

    public void completeAnalysis() {
        this.status = LeaseDocumentStatus.ANALYZED;
    }

    public void failAnalysis(String failureReason) {
        this.failureReason = failureReason;
        this.status = LeaseDocumentStatus.ANALYSIS_FAILED;
    }
}