package com.example.vivizip.notification.entity;

import com.example.vivizip.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "notification",
        indexes = @Index(name = "idx_user_id_id", columnList = "user_id, id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String body;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    // 탭 시 이동할 리소스 id (아티클 id / 매칭 id / 채팅방 id). 없을 수 있음
    @Column(name = "linked_resource_id")
    private Long linkedResourceId;

    private Notification(Long userId, NotificationType type, String title, String body, Long linkedResourceId) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.linkedResourceId = linkedResourceId;
    }

    public static Notification create(Long userId, NotificationType type, String title, String body, Long linkedResourceId) {
        return new Notification(userId, type, title, body, linkedResourceId);
    }

    public void markAsRead() {
        this.isRead = true;
    }
}