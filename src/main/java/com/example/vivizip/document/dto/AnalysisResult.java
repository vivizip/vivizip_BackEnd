package com.example.vivizip.document.dto;

// 서류 검토 결과의 공통 베이스. 서류 타입별 결과(예: LeaseContractAnalysisResult)가 이를 구현한다.
public interface AnalysisResult {
    String summary();
}
