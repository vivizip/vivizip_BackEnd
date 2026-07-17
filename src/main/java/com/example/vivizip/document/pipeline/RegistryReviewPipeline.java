package com.example.vivizip.document.pipeline;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.document.dto.RegistryAnalysisResult;
import com.example.vivizip.document.entity.AnalysisType;
import com.example.vivizip.llm.LlmExtractionException;
import com.example.vivizip.llm.LlmJsonExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegistryReviewPipeline implements DocumentAnalysisPipeline<RegistryAnalysisResult> {

    private static final String SCHEMA_NAME = "registry_analysis_result";

    private static final String SYSTEM_PROMPT = """
            당신은 한국 등기부등본을 분석하는 법률 보조 AI입니다.
            입력된 등기부등본 OCR 텍스트에서 아래 규칙에 따라 정보를 추출하세요.
            반드시 주어진 JSON 스키마 형식으로만 응답하세요. 추론 금지. 위험도 판단이나 조언 금지. JSON 외 출력 금지.

            문서 구조:
            - 표제부: 건물 표시 (소재지, 건물 구조, 용도 등)
            - 갑구: 소유권에 관한 사항 (소유권보존, 소유권이전, 가압류, 압류 등)
            - 을구: 소유권 이외의 권리에 관한 사항 (근저당권, 전세권 등)
            - 주요 등기사항 요약 (참고용): 마지막 페이지에 붙는 요약본. 말소되지 않은 사항만 정리됨.
              ├ 1. 소유지분현황 ( 갑구 ): 등기명의인, 최종지분, 순위번호
              ├ 2. 소유지분을 제외한 소유권에 관한 사항 ( 갑구 ): 가등기·압류 등 현존 사항
              └ 3. (근)저당권 및 전세권 등 ( 을구 ): 순위번호, 등기목적, 채권최고액

            ── 우선순위 규칙 ──
            OCR 텍스트에 "주요 등기사항 요약"이 포함되어 있는 경우 (hasSummaryPage = true):
              - ownerName: 요약본 "1. 소유지분현황"에서 "(소유자)" 표기된 등기명의인. 공동명의면 쉼표로 구분.
                예: "홍길동" / "홍길동, 이영희"
              - propertyAddress: 요약본 상단 "[집합건물]" 또는 "[건물]" 뒤에 기재된 주소. 없으면 표제부에서 추출.
              - hasMortgage / mortgageMaximumClaimAmount: 요약본 "3. (근)저당권 및 전세권 등"에서
                "근저당권설정"의 채권최고액만 합산. 요약본에 없는 근저당은 말소된 것이므로 포함하지 않음.
              - riskFlags: 요약본 "2번 항목"과 본문 갑구·을구 모두 확인. 요약본은 축약본이라 누락 가능성이
                있으므로 본문에서도 해당 문구가 존재하는지 반드시 교차 확인.

            OCR 텍스트에 "주요 등기사항 요약"이 없는 경우 (hasSummaryPage = false):
              - ownerName: 갑구에서 순위번호가 가장 큰 소유권 등기("소유권보존" 또는 "소유권이전")의 소유자명.
                공동명의면 쉼표로 구분. 예: "홍길동" / "홍길동, 이영희"
              - propertyAddress: 표제부의 [도로명주소] 우선, 없으면 소재지번. 집합건물은 동·호수 포함.
              - hasMortgage / mortgageMaximumClaimAmount: 을구에서 "N번○○등기말소" 패턴으로
                말소된 순위번호를 먼저 찾아 제외한 뒤 남은 근저당권설정의 채권최고액 합산.
              - riskFlags: 갑구·을구에서 동일하게 말소된 항목 제외 후 판단.

            ── 공통 규칙 ──
            1. propertyAddress: 집합건물(아파트·오피스텔 등)은 동·호수 반드시 포함.
            2. registeredAt: 소유권 등기의 "접수" 일자. 등기원인일(계약일)이 아님. YYYY.MM.DD 형식.
            3. issuedAt: 문서 하단의 "발행일" 또는 "열람일시" 뒤에 기재된 날짜. YYYY.MM.DD 형식으로 변환.
               예) "발행일 2002/06/21" → "2002.06.21", "열람일시 2024년 3월 5일" → "2024.03.05"
            4. mortgageMaximumClaimAmount: "금 195,000,000원" → 195000000 (숫자만, 원 단위). hasMortgage=false이면 반드시 null.
            5. buildingUsage: 표제부 건물내역에서 용도 표기를 그대로 추출. 층수 정보가 함께 표기된 경우 포함.
               예: "7층 도시형생활주택", "3층 근린생활시설", "아파트". 표제부에 용도 정보가 없으면 null.
            6. isResidential: buildingUsage에 아래 키워드 중 하나라도 포함되면 true, 그 외는 false.
               주거 키워드: 주택, 아파트, 연립, 다세대, 도시형생활주택, 주거용, 오피스텔
               (오피스텔은 "주거용 오피스텔"처럼 주거용이 명시된 경우만 true. 단순 "오피스텔"은 false.)
               buildingUsage가 null이거나 판단 불가면 false.
            7. hasSummaryPage: 텍스트에 "주요 등기사항 요약"이라는 문구가 있으면 true, 없으면 false.
            8. riskFlags 각 항목:
               - provisionalRegistration: "가등기" 존재 여부
               - trust: "신탁" 존재 여부
               - seizure: "압류" 존재 여부
               - provisionalSeizure: "가압류" 존재 여부
               - auctionStart: "경매개시결정" 존재 여부
               - leaseRegistration: "임차권등기명령" 존재 여부
               - jeonseRight: "전세권" 존재 여부
            """;

    private static final String JSON_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "propertyAddress": { "type": "string" },
                "ownerName": { "type": "string" },
                "registeredAt": { "type": "string" },
                "issuedAt": { "type": "string" },
                "hasMortgage": { "type": "boolean" },
                "mortgageMaximumClaimAmount": { "type": ["integer", "null"] },
                "buildingUsage": { "type": ["string", "null"] },
                "isResidential": { "type": "boolean" },
                "hasSummaryPage": { "type": "boolean" },
                "riskFlags": {
                  "type": "object",
                  "properties": {
                    "provisionalRegistration": { "type": "boolean" },
                    "trust": { "type": "boolean" },
                    "seizure": { "type": "boolean" },
                    "provisionalSeizure": { "type": "boolean" },
                    "auctionStart": { "type": "boolean" },
                    "leaseRegistration": { "type": "boolean" },
                    "jeonseRight": { "type": "boolean" }
                  },
                  "required": ["provisionalRegistration", "trust", "seizure", "provisionalSeizure", "auctionStart", "leaseRegistration", "jeonseRight"],
                  "additionalProperties": false
                }
              },
              "required": ["propertyAddress", "ownerName", "registeredAt", "issuedAt", "hasMortgage", "mortgageMaximumClaimAmount", "buildingUsage", "isResidential", "riskFlags", "hasSummaryPage"],
              "additionalProperties": false
            }
            """;

    private final LlmJsonExtractor llmJsonExtractor;

    @Override
    public AnalysisType type() {
        return AnalysisType.REGISTRY_ANALYSIS;
    }

    @Override
    public RegistryAnalysisResult analyze(String documentText) {
        String extractedText = extractText(documentText);
        String preprocessed = preprocess(extractedText);

        try {
            return llmJsonExtractor.extract(
                    SYSTEM_PROMPT, buildUserPrompt(preprocessed), SCHEMA_NAME, JSON_SCHEMA, RegistryAnalysisResult.class);
        } catch (LlmExtractionException e) {
            throw new GeneralException(ErrorStatus.REGISTRY_ANALYSIS_FAILED);
        }
    }

    private String extractText(String documentText) {
        return documentText;
    }

    private String preprocess(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new GeneralException(ErrorStatus.DOCUMENT_TEXT_EMPTY);
        }
        return rawText.strip().replaceAll("[ \\t]+", " ");
    }

    private String buildUserPrompt(String registryText) {
        return "다음은 등기부등본 OCR 텍스트입니다.\n\n" + registryText;
    }
}
