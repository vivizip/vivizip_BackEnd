package com.example.vivizip.document.pipeline;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.document.dto.BuildingLedgerAnalysisResult;
import com.example.vivizip.document.entity.AnalysisType;
import com.example.vivizip.llm.LlmExtractionException;
import com.example.vivizip.llm.LlmJsonExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BuildingLedgerReviewPipeline implements DocumentAnalysisPipeline<BuildingLedgerAnalysisResult> {

    private static final String SCHEMA_NAME = "building_ledger_analysis_result";

    private static final String SYSTEM_PROMPT = """
            당신은 한국 건축물대장을 검토하는 법률 보조 AI입니다.
            입력된 건축물대장 OCR 텍스트에서 아래 규칙에 따라 정보를 추출하세요.
            반드시 주어진 JSON 스키마 형식으로만 응답하세요.

            추출 규칙:
            1. issuedDate: "발급일 :" 뒤의 "YYYY년 MM월 DD일" → "YYYY.MM.DD" 형식으로 변환
            2. hasViolation: 문서에 "위반건축물"이라는 정확한 단어가 있으면 true, 없으면 false
            3. ownerName: "성명(명칭)" 항목의 사람 이름
            4. ownershipTransferDate: "소유권이전" 옆의 날짜 (YYYY.MM.DD 형식)
            5. address: "도로명주소" 뒤의 전체 주소 (괄호 포함)
            """;

    private static final String JSON_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "issuedDate": { "type": "string" },
                "hasViolation": { "type": "boolean" },
                "ownerName": { "type": "string" },
                "ownershipTransferDate": { "type": "string" },
                "address": { "type": "string" }
              },
              "required": ["issuedDate", "hasViolation", "ownerName", "ownershipTransferDate", "address"],
              "additionalProperties": false
            }
            """;

    private final LlmJsonExtractor llmJsonExtractor;

    @Override
    public AnalysisType type() {
        return AnalysisType.BUILDING_LEDGER_ANALYSIS;
    }

    @Override
    public BuildingLedgerAnalysisResult analyze(String documentText) {
        // OCR 자리 — 실제 이미지/PDF OCR은 추후 연동. MVP는 이미 추출된 텍스트를 그대로 통과시킨다.
        String extractedText = extractText(documentText);
        String preprocessed = preprocess(extractedText);

        try {
            return llmJsonExtractor.extract(
                    SYSTEM_PROMPT, buildUserPrompt(preprocessed), SCHEMA_NAME, JSON_SCHEMA, BuildingLedgerAnalysisResult.class);
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

    private String buildUserPrompt(String buildingLedgerText) {
        return "다음은 건축물대장 OCR 텍스트입니다.\n\n" + buildingLedgerText;
    }
}
