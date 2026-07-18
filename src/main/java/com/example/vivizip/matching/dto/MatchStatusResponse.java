package com.example.vivizip.matching.dto;

public record MatchStatusResponse(
        MatchApplicationStatus status
) {
    public static MatchStatusResponse of(MatchApplicationStatus status) {
        return new MatchStatusResponse(status);
    }
}