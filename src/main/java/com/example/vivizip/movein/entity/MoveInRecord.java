package com.example.vivizip.movein.entity;

import com.example.vivizip.movein.enums.DefectType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 입주 상태 기록장 (계약 후 단계)
 * - lease_case 와 1:1 연결 (leaseCaseId unique)
 * - memo: 사진 묶음 전체 코멘트 겸용
 * - 하자 칩(defects) / 사진(photos) 을 자식으로 1:N 보유
 */
@Entity
@Table(name = "move_in_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MoveInRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // LeaseCase 엔티티 만들어지기 전까지 임시로 ID만 보관
    @Column(name = "lease_case_id", nullable = false, unique = true)
    private Long leaseCaseId;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MoveInDefect> defects = new ArrayList<>();

    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<MoveInPhoto> photos = new ArrayList<>();

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public static MoveInRecord create(Long userId, Long leaseCaseId, String memo) {
        MoveInRecord r = new MoveInRecord();
        r.userId = userId;
        r.leaseCaseId = leaseCaseId;
        r.memo = memo;
        return r;
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }

    // ---- 하자 칩: 수정 시 전체 교체 ----
    public void replaceDefects(List<DefectType> types) {
        this.defects.clear();
        for (DefectType type : types) {
            this.defects.add(new MoveInDefect(this, type));
        }
    }

    // ---- 사진 추가/삭제 ----
    public void addPhoto(String fileUrl, String s3Key) {
        this.photos.add(new MoveInPhoto(this, fileUrl, s3Key, this.photos.size()));
    }

    public void removePhoto(MoveInPhoto photo) {
        this.photos.remove(photo);
    }
}
