package com.example.vivizip.document.dto.중개대상물;

import java.util.List;

public record BasicInfoResult(
        Boolean matchesRegistry,
        String owner,
        String roadAddress,
        List<HighlightRegion> regions
) {
}