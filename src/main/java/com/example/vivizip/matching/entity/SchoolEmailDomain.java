package com.example.vivizip.matching.entity;

import com.example.vivizip.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "school_email_domains",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_school_email_domain", columnNames = {"email_domain"})
        },
        indexes = {
                @Index(name = "idx_school_email_domain_school", columnList = "school_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SchoolEmailDomain extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 학교 FK
    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "email_domain", nullable = false)
    private String emailDomain;
}