package com.example.vivizip.document.dto.중개대상물;

public record HighlightRegion(
        int documentPage,
        BoundingBox box,
        String label
) {
}