package com.example.vivizip.document.pipeline;

import com.example.vivizip.document.dto.임대차계약서.RiskyClauseListWrapper;
import com.example.vivizip.document.dto.임대차계약서.RiskyClauseRaw;
import com.example.vivizip.llm.LlmExtractionException;
import com.example.vivizip.llm.LlmJsonExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

// 특약사항 중 외국인 세입자에게 불리한 조항을 AI가 판별하고 설명·대안을 제시한다.
// specialClauses가 비어 있으면 호출하지 않는다.
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskyClausePipeline {

    private static final String SCHEMA_NAME = "risky_clause_list";

    private static final String SYSTEM_PROMPT = """
            당신은 한국 부동산 임대차 계약 전문가입니다.
            독자는 한국 부동산 제도를 모르는 외국인 세입자입니다.

            아래에 임대차 계약서의 특약사항 목록이 주어집니다.
            세입자에게 불리한 조항만 골라 설명과 대안을 제시하세요. 불리한 조항이 없으면 빈 배열을 반환하세요.

            판단 기준 (여기 없어도 불리하면 잡을 것):
            - 전입신고나 확정일자를 막거나 미루게 하는 조항 ← 가장 위험, 최우선 검출
            - 보증금 반환에 조건을 붙이는 조항 (예: 다음 세입자 입주 후 반환)
            - 원상복구 범위가 과도한 조항 (통상적인 마모까지 세입자 부담)
            - 임대인의 수선 의무를 면제하는 조항
            - 관리비 외 추가 비용을 세입자에게 부담시키는 조항
            - 계약 해지 시 세입자에게만 위약금을 부과하는 조항

            반려동물 금지, 흡연 금지 같은 일반적인 생활 규칙 조항은 불리하지 않습니다. 반환하지 마세요.

            작성 규칙:
            - originalText: 특약사항 원문을 그대로 옮길 것. 요약·수정·띄어쓰기 교정 절대 금지 (좌표 검색에 사용됨)
            - reason: 위험 유형의 이름을 그대로 반복하지 마세요.
              이 조항 때문에 세입자에게 구체적으로 어떤 일이 생기는지 쓰세요.
              조항에 적힌 구체적인 내용(품목, 금액, 조건)을 반드시 인용하세요.
              나쁜 예: "원상복구 범위가 과도할 수 있어요" (유형 이름만 반복)
              좋은 예: "도배·장판이 낡거나 얼룩졌을 뿐인데도 퇴거할 때 전면 교체 비용을 세입자가 전부 내야 해서, 퇴거 시 수백만원을 청구받을 수 있어요"
              나쁜 예: "계약 해지 시 세입자에게만 위약금을 부과하는 조항일 수 있어요" (유형 이름만 반복)
              좋은 예: "신규 세입자가 구해지지 않으면 퇴거 후에도 월세와 관리비를 계속 내야 해서, 몇 달치 이중 부담이 생길 수 있어요"
            - suggestion: 이 조항의 가장 큰 문제를 해결해야 합니다.
              세입자가 최악의 경우 무엇을 잃는지 먼저 파악한 뒤, 그것을 막는 구체적 문구를 제안하세요.
              실제 계약서에 넣을 수 있는 문구여야 합니다.
              나쁜 예: "협의가 필요해요", "확인하세요", "임대인 책임인 경우 면제 문구 추가"
              좋은 예: "'단, 통상적인 사용에 따른 마모(도배·장판의 자연 변색·오염 등)는 제외한다'는 문장을 추가해달라고 요청하세요"
              좋은 예: "'잔여 임대기간 종료 후 30일 이내에 보증금을 반환한다'는 문장으로 교체해달라고 요청하세요"
            - 법 조항 번호 쓰지 말 것
            - 한국어로 작성

            === 작성 예시 ===

            [특약 원문] "퇴실시 임차인은 도배 및 장판을 새것으로 교체하여 원상복구한다."
            reason: "도배·장판이 낡거나 얼룩졌을 뿐인데도 퇴거할 때 전면 교체 비용을 세입자가 전부 내야 해서, 퇴거 시 수백만원을 청구받을 수 있어요"
            suggestion: "'단, 통상적인 사용에 따른 자연 마모(도배·장판의 변색·오염 등)는 제외한다'는 문장을 추가해달라고 요청하세요"

            [특약 원문] "신규세입자 입주전까지 월차임 및 관리비를 납입한다."
            reason: "계약이 끝나고 이사를 나갔는데도 다음 세입자가 구해지지 않으면 월세와 관리비를 계속 내야 해서, 수개월치 이중 부담이 생길 수 있어요"
            suggestion: "'임대차 기간 종료일 이후에는 임차인의 월차임 납입 의무가 없다'는 문장으로 교체해달라고 요청하세요"

            반드시 주어진 JSON 스키마 형식의 순수 JSON으로만 응답하세요. 다른 설명은 붙이지 마세요.
            """;

    private static final String JSON_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "items": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "originalText": { "type": "string" },
                      "reason":       { "type": "string" },
                      "suggestion":   { "type": "string" }
                    },
                    "required": ["originalText", "reason", "suggestion"],
                    "additionalProperties": false
                  }
                }
              },
              "required": ["items"],
              "additionalProperties": false
            }
            """;

    private final LlmJsonExtractor llmJsonExtractor;

    public List<RiskyClauseRaw> analyze(List<String> specialClauses) {
        try {
            RiskyClauseListWrapper result = llmJsonExtractor.extract(
                    SYSTEM_PROMPT, buildUserPrompt(specialClauses), SCHEMA_NAME, JSON_SCHEMA,
                    RiskyClauseListWrapper.class);
            return result.items() != null ? result.items() : List.of();
        } catch (LlmExtractionException e) {
            log.warn("[RiskyClausePipeline] 위험 특약 분석 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildUserPrompt(List<String> specialClauses) {
        StringBuilder sb = new StringBuilder("다음은 임대차계약서의 특약사항 목록입니다. 세입자에게 불리한 조항을 분석해주세요.\n\n");
        for (int i = 0; i < specialClauses.size(); i++) {
            sb.append((i + 1)).append(". ").append(specialClauses.get(i)).append("\n");
        }
        return sb.toString();
    }
}