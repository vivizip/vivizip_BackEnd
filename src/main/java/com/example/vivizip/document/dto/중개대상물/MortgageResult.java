package com.example.vivizip.document.dto.중개대상물;

import java.util.List;

public record MortgageResult(
        Boolean matchesRegistry,
        List<HighlightRegion> regions
) {
}