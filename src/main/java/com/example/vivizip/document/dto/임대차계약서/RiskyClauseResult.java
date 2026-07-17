package com.example.vivizip.document.dto.임대차계약서;

import com.example.vivizip.document.dto.중개대상물.HighlightRegion;

import java.util.List;

public record RiskyClauseResult(
        String originalText,
        String reason,
        String suggestion,
        List<HighlightRegion> regions
) {
}