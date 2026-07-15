package com.example.vivizip.document.pipeline;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.document.dto.LeaseContractAnalysisResult;
import com.example.vivizip.document.entity.AnalysisType;
import com.example.vivizip.llm.LlmExtractionException;
import com.example.vivizip.llm.LlmJsonExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeaseContractReviewPipeline implements DocumentAnalysisPipeline<LeaseContractAnalysisResult> {

    private static final String SCHEMA_NAME = "lease_contract_analysis_result";

    private static final String SYSTEM_PROMPT = """
            당신은 한국 부동산 임대차 계약서를 검토하는 법률 보조 AI입니다.
            입력된 계약서 본문을 읽고, 임차인 입장에서 위험할 수 있는 조항을 찾아내며,
            계약 전 반드시 확인해야 할 체크리스트를 제시하세요.
            반드시 주어진 JSON 스키마 형식으로만 응답하세요.
            """;

    private static final String JSON_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "summary": { "type": "string" },
                "riskClauses": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "clause": { "type": "string" },
                      "riskLevel": { "type": "string", "enum": ["HIGH", "MEDIUM", "LOW"] },
                      "reason": { "type": "string" }
                    },
                    "required": ["clause", "riskLevel", "reason"],
                    "additionalProperties": false
                  }
                },
                "checklist": {
                  "type": "array",
                  "items": { "type": "string" }
                }
              },
              "required": ["summary", "riskClauses", "checklist"],
              "additionalProperties": false
            }
            """;

    private final LlmJsonExtractor llmJsonExtractor;

    @Override
    public AnalysisType type() {
        return AnalysisType.LEASE_CONTRACT_ANALYSIS;
    }

    @Override
    public LeaseContractAnalysisResult analyze(String documentText) {
        // OCR 자리 — 실제 이미지/PDF OCR은 추후 연동. MVP는 이미 추출된 텍스트를 그대로 통과시킨다.
        String extractedText = extractText(documentText);
        String preprocessed = preprocess(extractedText);

        try {
            return llmJsonExtractor.extract(
                    SYSTEM_PROMPT, buildUserPrompt(preprocessed), SCHEMA_NAME, JSON_SCHEMA, LeaseContractAnalysisResult.class);
        } catch (LlmExtractionException e) {
            throw new GeneralException(ErrorStatus.DOCUMENT_LLM_RESPONSE_INVALID);
        }
    }

    private String extractText(String documentText) {
        return documentText;
    }

    private String preprocess(String rawText) {
        String trimmed = rawText.strip();
        if (trimmed.isEmpty()) {
            throw new GeneralException(ErrorStatus.DOCUMENT_TEXT_EMPTY);
        }
        return trimmed.replaceAll("[ \\t]+", " ");
    }

    private String buildUserPrompt(String contractText) {
        return "다음은 임대차 계약서 본문입니다.\n\n" + contractText;
    }
}
