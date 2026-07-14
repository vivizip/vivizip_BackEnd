package com.example.vivizip.document.entity;

public enum LeaseDocumentType {
    REGISTRY,
    BUILDING_LEDGER,
    BROKERAGE_CONFIRMATION,
    LEASE_CONTRACT;

    public AnalysisType analysisType() {
        return switch (this) {
            case REGISTRY -> AnalysisType.REGISTRY_ANALYSIS;
            case BUILDING_LEDGER -> AnalysisType.BUILDING_LEDGER_ANALYSIS;
            case BROKERAGE_CONFIRMATION -> AnalysisType.BROKERAGE_DOCUMENT_ANALYSIS;
            case LEASE_CONTRACT -> AnalysisType.LEASE_CONTRACT_ANALYSIS;
        };
    }
}