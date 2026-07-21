package com.example.vivizip.document.entity;

import com.example.vivizip.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "lease_case")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeaseCase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 100)
    private String name;

    @Column(name = "road_address", length = 255)
    private String roadAddress;

    @Column(name = "detail_address", length = 255)
    private String detailAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LeaseCaseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_stage", nullable = false, length = 30)
    private ContractStage contractStage = ContractStage.BEFORE_CONTRACT;

    private LeaseCase(Long userId, String name, String roadAddress, String detailAddress) {
        this.userId = userId;
        this.name = name;
        this.roadAddress = roadAddress;
        this.detailAddress = detailAddress;
        this.status = LeaseCaseStatus.ACTIVE;
    }

    public static LeaseCase create(Long userId, String name, String roadAddress, String detailAddress) {
        return new LeaseCase(userId, name, roadAddress, detailAddress);
    }

    public void complete() {
        this.status = LeaseCaseStatus.COMPLETED;
    }

    public void delete() {
        this.status = LeaseCaseStatus.DELETED;
    }

    // 이미 더 앞선(높은) 단계로 가 있으면 무시 — 재분석 등으로 단계가 뒤로 내려가지 않도록 한다.
    public void advanceContractStage(ContractStage newStage) {
        if (newStage.ordinal() > this.contractStage.ordinal()) {
            this.contractStage = newStage;
        }
    }
}