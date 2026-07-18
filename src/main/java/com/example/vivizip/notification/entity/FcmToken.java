package com.example.vivizip.notification.entity;

import com.example.vivizip.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 유저당 여러 대의 디바이스에서 로그인할 수 있어 (userId, token) 여러 행이 나올 수 있다.
// token 자체는 기기(앱 설치)당 유일하므로 unique 제약을 둔다.
@Getter
@Entity
@Table(name = "fcm_token", uniqueConstraints = @UniqueConstraint(columnNames = "token"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String token;

    private FcmToken(Long userId, String token) {
        this.userId = userId;
        this.token = token;
    }

    public static FcmToken create(Long userId, String token) {
        return new FcmToken(userId, token);
    }

    // 같은 기기에서 다른 계정으로 로그인한 경우, 기존 토큰 행의 소유자를 바꿔치기한다.
    public void reassignTo(Long userId) {
        this.userId = userId;
    }
}