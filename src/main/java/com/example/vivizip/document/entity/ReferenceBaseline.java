package com.example.vivizip.document.entity;

import com.example.vivizip.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "reference_baseline")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReferenceBaseline extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lease_case_id", nullable = false, unique = true)
    private Long leaseCaseId;

    // 등기부 갑구에서 확정된 소유자명
    @Column(name = "owner_name", nullable = false, length = 100)
    private String ownerName;

    // 등기부 표제부 부동산 주소
    @Column(name = "property_address", nullable = false, length = 500)
    private String propertyAddress;

    @Column(name = "has_mortgage", nullable = false)
    private boolean hasMortgage;

    @Column(name = "mortgage_maximum_claim_amount")
    private Long mortgageMaximumClaimAmount;

    // 중개대상물 확인·설명서에서 추출한 매도인(임대인) 성명. 등기부 ownerName과 표기가 다를 수 있어 별도 컬럼으로 둔다.
    @Column(name = "brokerage_document_owner_name", length = 100)
    private String brokerageDocumentOwnerName;

    // 중개대상물 도로명 주소
    @Column(name = "brokerage_document_address", length = 500)
    private String brokerageDocumentAddress;

    @Column(name = "deposit")
    private Long deposit;

    @Column(name = "monthly_rent")
    private Long monthlyRent;

    private ReferenceBaseline(Long leaseCaseId, String ownerName, String propertyAddress,
                               boolean hasMortgage, Long mortgageMaximumClaimAmount) {
        if (!hasMortgage && mortgageMaximumClaimAmount != null) {
            throw new IllegalArgumentException("hasMortgage가 false이면 mortgageMaximumClaimAmount는 null이어야 합니다.");
        }
        this.leaseCaseId = leaseCaseId;
        this.ownerName = ownerName;
        this.propertyAddress = propertyAddress;
        this.hasMortgage = hasMortgage;
        this.mortgageMaximumClaimAmount = mortgageMaximumClaimAmount;
    }

    public static ReferenceBaseline create(Long leaseCaseId, String ownerName, String propertyAddress,
                                            boolean hasMortgage, Long mortgageMaximumClaimAmount) {
        return new ReferenceBaseline(leaseCaseId, ownerName, propertyAddress, hasMortgage, mortgageMaximumClaimAmount);
    }

    public void update(String ownerName, String propertyAddress, boolean hasMortgage, Long mortgageMaximumClaimAmount) {
        if (!hasMortgage && mortgageMaximumClaimAmount != null) {
            throw new IllegalArgumentException("hasMortgage가 false이면 mortgageMaximumClaimAmount는 null이어야 합니다.");
        }
        this.ownerName = ownerName;
        this.propertyAddress = propertyAddress;
        this.hasMortgage = hasMortgage;
        this.mortgageMaximumClaimAmount = mortgageMaximumClaimAmount;
    }

    public void updateFromBrokerageDocument(String brokerageDocumentOwnerName, String brokerageDocumentAddress,
                                             Long deposit, Long monthlyRent) {
        this.brokerageDocumentOwnerName = brokerageDocumentOwnerName;
        this.brokerageDocumentAddress = brokerageDocumentAddress;
        this.deposit = deposit;
        this.monthlyRent = monthlyRent;
    }
}