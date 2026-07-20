package com.example.vivizip.chat.entity;

import com.example.vivizip.chat.enums.ChatRoomStatus;
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

    @Column(name = "match_id", unique = true)
    private Long matchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ChatRoomStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 참여자별 마지막으로 읽은 메시지 ID (null = 한 번도 읽지 않음)
    @Column(name = "supporter_last_read_id")
    private Long supporterLastReadId;

    @Column(name = "student_last_read_id")
    private Long studentLastReadId;

    private ChatRoom(Long supporterId, Long studentId, Long matchId) {
        this.supporterId = supporterId;
        this.studentId = studentId;
        this.matchId = matchId;
        this.status = ChatRoomStatus.ACTIVE;
    }

    public static ChatRoom of(Long supporterId, Long studentId) {
        return new ChatRoom(supporterId, studentId, null);
    }

    public static ChatRoom forMatch(Long matchId, Long supporterId, Long studentId) {
        return new ChatRoom(supporterId, studentId, matchId);
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // 이 방에 속한 유저인지 검증 (권한 체크용)
    public boolean hasParticipant(Long userId) {
        return supporterId.equals(userId) || studentId.equals(userId);
    }

    public void close() {
        this.status = ChatRoomStatus.CLOSED;
    }

    // 호출자의 마지막 읽은 메시지 ID 갱신
    public void updateLastRead(Long userId, Long messageId) {
        if (supporterId.equals(userId)) {
            this.supporterLastReadId = messageId;
        } else if (studentId.equals(userId)) {
            this.studentLastReadId = messageId;
        }
    }

    // 상대방의 마지막 읽은 메시지 ID 반환
    public Long getCounterpartLastReadId(Long myUserId) {
        if (supporterId.equals(myUserId)) return studentLastReadId;
        return supporterLastReadId;
    }
}