package com.example.vivizip.appointment.entity;

import com.example.vivizip.appointment.enums.AppointmentStatus;
import com.example.vivizip.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "appointment",
        indexes = @Index(name = "idx_room_scheduled", columnList = "chat_room_id, scheduled_at")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Appointment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어느 채팅방의 약속인지
    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    // 누가 생성했는지 (기록용)
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    // 약속 일시 (날짜 + 시간)
    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    // 약속 장소 (카카오 장소 검색 결과)
    @Column(name = "place_name", nullable = false)
    private String placeName;

    @Column(name = "place_address")
    private String placeAddress;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    private Appointment(Long chatRoomId, Long createdBy, LocalDateTime scheduledAt,
                        String placeName, String placeAddress, Double latitude, Double longitude) {
        this.chatRoomId = chatRoomId;
        this.createdBy = createdBy;
        this.scheduledAt = scheduledAt;
        this.placeName = placeName;
        this.placeAddress = placeAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = AppointmentStatus.SCHEDULED;   // 생성 즉시 확정
    }

    public static Appointment of(Long chatRoomId, Long createdBy, LocalDateTime scheduledAt,
                                 String placeName, String placeAddress,
                                 Double latitude, Double longitude) {
        return new Appointment(chatRoomId, createdBy, scheduledAt,
                placeName, placeAddress, latitude, longitude);
    }

    // null이 아닌 값만 갱신 (PATCH 부분 수정)
    public void update(LocalDateTime scheduledAt, String placeName, String placeAddress,
                       Double latitude, Double longitude) {
        if (scheduledAt != null) this.scheduledAt = scheduledAt;
        if (placeName != null) this.placeName = placeName;
        if (placeAddress != null) this.placeAddress = placeAddress;
        if (latitude != null) this.latitude = latitude;
        if (longitude != null) this.longitude = longitude;
    }

    public void cancel() {
        this.status = AppointmentStatus.CANCELED;
    }
}
