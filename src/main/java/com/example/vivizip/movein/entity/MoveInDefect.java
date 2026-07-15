package com.example.vivizip.movein.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.example.vivizip.movein.enums.DefectType;
import java.time.LocalDateTime;

/**
 * 하자 칩 (입주 기록 1:N)
 * - 선택된 칩 하나 = row 하나
 */
@Entity
@Table(name = "move_in_defect")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MoveInDefect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", nullable = false)
    private MoveInRecord record;

    @Enumerated(EnumType.STRING)
    @Column(name = "defect_type", length = 30, nullable = false)
    private DefectType defectType;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public MoveInDefect(MoveInRecord record, DefectType defectType) {
        this.record = record;
        this.defectType = defectType;
    }
}
