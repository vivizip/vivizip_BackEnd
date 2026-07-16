package com.example.vivizip.movein.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 입주 기록 사진 (입주 기록 1:N)
 * - sortOrder 로 캐러셀 순서 관리
 * - s3Key: S3 삭제 시 사용
 */
@Entity
@Table(name = "move_in_photo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MoveInPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", nullable = false)
    private MoveInRecord record;

    @Column(name = "file_url", length = 500, nullable = false)
    private String fileUrl;

    @Column(name = "s3_key", length = 500, nullable = false)
    private String s3Key;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public MoveInPhoto(MoveInRecord record, String fileUrl, String s3Key, int sortOrder) {
        this.record = record;
        this.fileUrl = fileUrl;
        this.s3Key = s3Key;
        this.sortOrder = sortOrder;
    }
}
