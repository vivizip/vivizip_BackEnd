package com.example.vivizip.ocr.dto;

import java.util.List;

public record KeywordSearchResponse(

        String keyword,
        int totalCount,
        List<MatchResult> matches
) {
    public record MatchResult(
            int pageIndex,
            String inferText,
            List<Vertex> vertices
    ) {}

    public record Vertex(Double x, Double y) {}
}
