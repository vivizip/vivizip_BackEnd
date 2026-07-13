package com.example.vivizip.matching.entity;

import com.example.vivizip.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "matches",
        indexes = {
                @Index(name = "idx_matches_student_status", columnList = "student_id, status"),
                @Index(name = "idx_matches_supporter_status", columnList = "supporter_id, status"),
                @Index(name = "idx_matches_canceled_by", columnList = "canceled_by_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Match extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 유학생 FK
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    // 서포터즈 FK
    @Column(name = "supporter_id", nullable = false)
    private Long supporterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStatus status;

    // 취소/재매칭 요청한 사용자 FK
    @Column(name = "canceled_by_id")
    private Long canceledById;

    @Column(name = "cancel_reason")
    private String cancelReason;

    public static Match createMatched(Long studentId, Long supporterId) {
        return Match.builder()
                .studentId(studentId)
                .supporterId(supporterId)
                .status(MatchStatus.MATCHED)
                .build();
    }

    public void cancel(Long canceledById, String cancelReason) {
        this.status = MatchStatus.CANCELED;
        this.canceledById = canceledById;
        this.cancelReason = cancelReason;
    }
}