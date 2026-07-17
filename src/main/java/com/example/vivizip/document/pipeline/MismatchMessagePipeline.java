package com.example.vivizip.document.pipeline;

import com.example.vivizip.document.dto.임대차계약서.MismatchMessages;
import com.example.vivizip.llm.LlmExtractionException;
import com.example.vivizip.llm.LlmJsonExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 중개대상물과 계약서의 금액이 불일치할 때, 세입자가 중개인에게 읽어 말할 확인 질문을 생성한다.
// depositMatched / monthlyRentMatched 중 하나라도 false일 때만 호출한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class MismatchMessagePipeline {

    private static final String SCHEMA_NAME = "mismatch_messages";

    private static final String SYSTEM_PROMPT = """
            당신은 외국인 세입자를 돕는 부동산 계약 도우미입니다.
            중개대상물 확인·설명서와 임대차계약서의 금액이 서로 다를 때,
            세입자가 중개인에게 그대로 읽어서 말할 수 있는 확인 질문을 만들어주세요.

            작성 규칙:
            - "~예요", "~인가요?" 처럼 부드럽고 간결한 말투
            - 외국인이 읽기 쉽게 짧고 명확하게
            - 두 금액을 모두 언급할 것
            - 1~2문장으로 작성
            - 법률 용어 사용 금지
            - depositMessage: 보증금 불일치가 있을 때만 작성. 없으면 null.
            - monthlyRentMessage: 월세 불일치가 있을 때만 작성. 없으면 null.

            반드시 주어진 JSON 스키마 형식의 순수 JSON으로만 응답하세요. 다른 설명은 붙이지 마세요.
            """;

    private static final String JSON_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "depositMessage":     { "type": ["string", "null"] },
                "monthlyRentMessage": { "type": ["string", "null"] }
              },
              "required": ["depositMessage", "monthlyRentMessage"],
              "additionalProperties": false
            }
            """;

    private final LlmJsonExtractor llmJsonExtractor;

    public MismatchMessages generate(
            boolean depositMismatched, Long brokerageDeposit, Long contractDeposit,
            boolean monthlyRentMismatched, Long brokerageMonthlyRent, Long contractMonthlyRent) {
        try {
            return llmJsonExtractor.extract(
                    SYSTEM_PROMPT,
                    buildUserPrompt(depositMismatched, brokerageDeposit, contractDeposit,
                            monthlyRentMismatched, brokerageMonthlyRent, contractMonthlyRent),
                    SCHEMA_NAME, JSON_SCHEMA, MismatchMessages.class);
        } catch (LlmExtractionException e) {
            log.warn("[MismatchMessagePipeline] 불일치 멘트 생성 실패: {}", e.getMessage());
            return new MismatchMessages(null, null);
        }
    }

    private String buildUserPrompt(
            boolean depositMismatched, Long brokerageDeposit, Long contractDeposit,
            boolean monthlyRentMismatched, Long brokerageMonthlyRent, Long contractMonthlyRent) {

        StringBuilder sb = new StringBuilder("다음은 중개대상물 확인·설명서와 임대차계약서의 금액 비교 결과입니다.\n\n[불일치 항목]\n");

        if (depositMismatched) {
            sb.append(String.format("보증금: 중개대상물 %,d원 → 계약서 %s원 (불일치)%n",
                    brokerageDeposit,
                    contractDeposit != null ? String.format("%,d", contractDeposit) : "미기재"));
        } else {
            sb.append("보증금: 동일 (불일치 없음)\n");
        }

        if (monthlyRentMismatched) {
            sb.append(String.format("월세: 중개대상물 %,d원 → 계약서 %s원 (불일치)%n",
                    brokerageMonthlyRent,
                    contractMonthlyRent != null ? String.format("%,d", contractMonthlyRent) : "미기재"));
        } else {
            sb.append("월세: 동일 (불일치 없음)\n");
        }

        sb.append("\n불일치 항목에 대해서만 depositMessage 또는 monthlyRentMessage를 작성해주세요. 불일치가 없는 항목은 null로 두세요.");
        return sb.toString();
    }
}