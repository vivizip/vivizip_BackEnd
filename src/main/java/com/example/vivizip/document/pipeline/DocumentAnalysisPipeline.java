package com.example.vivizip.document.pipeline;

import com.example.vivizip.document.dto.AnalysisResult;
import com.example.vivizip.document.entity.AnalysisType;

// 서류 검토 파이프라인 인터페이스: 문서 → OCR → 전처리 → LLM 추출 → 스키마 검증. (저장은 service 계층 책임)
// 서류 타입별로 이 인터페이스를 구현해 자신만의 프롬프트/스키마/결과 타입을 갖는다.
public interface DocumentAnalysisPipeline<T extends AnalysisResult> {
    AnalysisType type();

    T analyze(String documentText);
}
