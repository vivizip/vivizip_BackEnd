package com.example.vivizip.document.dto.임대차계약서;

import java.util.List;

public record LeaseContractAnalysisResponse(
        LeaseContractBasicInfoResult basicInfo,
        LeaseContractCostResult cost,
        List<RiskyClauseResult> riskyClauses
) {
}