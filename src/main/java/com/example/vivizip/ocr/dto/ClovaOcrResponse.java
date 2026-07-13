package com.example.vivizip.ocr.dto;

import java.util.List;

public record ClovaOcrResponse(
        String version,
        String requestId,
        Long timestamp,
        List<Image> images
) {
    public record Image(
            String uid,
            String name,
            String inferResult,
            String message,
            List<Field> fields,
            List<Table> tables      // ← 추가
    ) {}

    public record Field(
            String inferText,
            Double inferConfidence,
            String type,
            Boolean lineBreak,
            BoundingPoly boundingPoly
    ) {}

    // ↓↓↓ 여기부터 표 관련 record 추가 ↓↓↓
    public record Table(
            List<Cell> cells
    ) {}

    public record Cell(
            Integer rowIndex,           // 셀의 행 번호
            Integer columnIndex,        // 셀의 열 번호
            Integer rowSpan,
            Integer columnSpan,
            List<CellTextLine> cellTextLines
    ) {}

    public record CellTextLine(
            List<CellWord> cellWords
    ) {}

    public record CellWord(
            String inferText,
            Double inferConfidence
    ) {}
    // ↑↑↑ 여기까지 ↑↑↑

    public record BoundingPoly(List<Vertex> vertices) {}

    public record Vertex(Double x, Double y) {}
}
