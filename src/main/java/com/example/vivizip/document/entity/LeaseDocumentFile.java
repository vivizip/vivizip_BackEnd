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
@Table(name = "lease_document_file")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeaseDocumentFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lease_document_id", nullable = false)
    private Long leaseDocumentId;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;

    @Column(name = "page_order", nullable = false)
    private int pageOrder;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    private LeaseDocumentFile(Long leaseDocumentId, String s3Key, int pageOrder) {
        this.leaseDocumentId = leaseDocumentId;
        this.s3Key = s3Key;
        this.pageOrder = pageOrder;
    }

    public static LeaseDocumentFile create(Long leaseDocumentId, String s3Key, int pageOrder) {
        return new LeaseDocumentFile(leaseDocumentId, s3Key, pageOrder);
    }
}
