package com.example.vivizip.document.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RegistryAnalysisResult(
        @NotBlank String propertyAddress,
        @NotBlank String ownerName,
        @NotBlank String registeredAt,
        @NotBlank String issuedAt,
        @NotNull Boolean hasMortgage,
        Long mortgageMaximumClaimAmount,
        String buildingUsage,
        @NotNull Boolean isResidential,
        @NotNull @Valid RiskFlags riskFlags,
        @NotNull Boolean hasSummaryPage
) implements AnalysisResult {

    @Override
    public String summary() {
        List<RegistryRiskType> detected = riskFlags.detected();
        if (detected.isEmpty()) {
            return "특이사항이 없습니다.";
        }
        return detected.stream()
                .map(RegistryRiskType::getLabel)
                .collect(Collectors.joining(", ")) + "이(가) 탐지되었습니다.";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RiskFlags(
            boolean provisionalRegistration,
            boolean trust,
            boolean seizure,
            boolean provisionalSeizure,
            boolean auctionStart,
            boolean leaseRegistration,
            boolean jeonseRight
    ) {
        public List<RegistryRiskType> detected() {
            List<RegistryRiskType> result = new ArrayList<>();
            if (provisionalRegistration) result.add(RegistryRiskType.PROVISIONAL_REGISTRATION);
            if (trust) result.add(RegistryRiskType.TRUST);
            if (seizure) result.add(RegistryRiskType.SEIZURE);
            if (provisionalSeizure) result.add(RegistryRiskType.PROVISIONAL_SEIZURE);
            if (auctionStart) result.add(RegistryRiskType.AUCTION_START);
            if (leaseRegistration) result.add(RegistryRiskType.LEASE_REGISTRATION);
            if (jeonseRight) result.add(RegistryRiskType.JEONSE_RIGHT);
            return result;
        }
    }
}
