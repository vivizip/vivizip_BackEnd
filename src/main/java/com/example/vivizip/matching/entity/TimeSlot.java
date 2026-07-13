package com.example.vivizip.matching.entity;

import com.example.vivizip.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "time_slots",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_time_slot_user_day_period",
                        columnNames = {"user_id", "day", "period"}
                )
        },
        indexes = {
                @Index(name = "idx_time_slot_user", columnList = "user_id"),
                @Index(name = "idx_time_slot_day_period", columnList = "day, period")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TimeSlot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 유저 FK
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DayOfWeekType day;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TimePeriod period;

    public static TimeSlot create(Long userId, DayOfWeekType day, TimePeriod period) {
        return TimeSlot.builder()
                .userId(userId)
                .day(day)
                .period(period)
                .build();
    }
}