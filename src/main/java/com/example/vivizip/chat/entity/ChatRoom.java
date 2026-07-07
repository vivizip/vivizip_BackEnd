package com.example.vivizip.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "chat_room",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_supporter_student",
                columnNames = {"supporter_id", "student_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supporter_id", nullable = false)
    private Long supporterId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private ChatRoom(Long supporterId, Long studentId) {
        this.supporterId = supporterId;
        this.studentId = studentId;
    }

    public static ChatRoom of(Long supporterId, Long studentId) {
        return new ChatRoom(supporterId, studentId);
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // 이 방에 속한 유저인지 검증 (권한 체크용)
    public boolean hasParticipant(Long userId) {
        return supporterId.equals(userId) || studentId.equals(userId);
    }
}