package com.example.vivizip.user.entity;

import com.example.vivizip.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // kakao_id varchar UNIQUE
    // 카카오 ID가 숫자로 오긴 하지만, 외부 플랫폼 ID는 String으로 저장하는 것도 많이 씀
    @Column(name = "kakao_id", nullable = false, unique = true)
    private String kakaoId;

    // email varchar UNIQUE
    @Column(unique = true)
    private String email;

    // nickname varchar
    @Column(nullable = false)
    private String name;

    // profile_image varchar
    @Column(name = "profile_image")
    private String profileImage;

    // role varchar
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // language varchar DEFAULT 'KOREAN'
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Language language = Language.KOREAN;

    @Enumerated(EnumType.STRING)
    @Column(name = "nationality_code", length = 30)
    private Nationality nationality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Gender gender = Gender.NOT_SPECIFIED;

    // school_id bigint FK
    @Column(name = "school_id")
    private Long schoolId;

    // school_verified boolean
    @Column(name = "school_verified", nullable = false)
    @Builder.Default
    private Boolean schoolVerified = false;

    // status varchar
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    public void updateProfile(String profileImage) {
        if (profileImage != null) this.profileImage = profileImage;
    }

    public void updateLanguage(Language language) {
        this.language = language;
    }

    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
    }
}