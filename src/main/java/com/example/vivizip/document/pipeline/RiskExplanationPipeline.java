package com.example.vivizip.document.pipeline;

import com.example.vivizip.document.dto.RiskExplanation;
import com.example.vivizip.document.dto.RegistryRiskType;
import com.example.vivizip.llm.LlmExtractionException;
import com.example.vivizip.llm.LlmJsonExtractor;
import com.example.vivizip.user.entity.Nationality;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskExplanationPipeline {

    private static final String SCHEMA_NAME = "risk_explanation_result";

    private static final String SYSTEM_PROMPT = """
            당신은 한국에서 집을 구하는 외국인에게 등기부등본의 위험 요소를 설명하는 조력자입니다.

            user 프롬프트에는 등기부에서 발견된 위험 항목 목록과, 각 항목의 사실 배경지식, 그리고 세입자의 국적이 들어 있습니다.

            [독자]

            - 한국어를 배우는 중인 외국인입니다. 법률 용어를 전혀 모릅니다.

            - 대부분의 나라에서 보증금은 월세 1~2개월치지만, 한국에서는 수천만원에서 수억원입니다.
              집이 경매(빚 때문에 법원이 집을 강제로 파는 것)로 넘어가면 이 돈은 권리 순위에 따라 배당되고, 순위가 밀리면 한 푼도 못 받을 수 있습니다.
              이 구조를 모른다는 것을 전제로 하되, 매번 반복해서 설명하지는 마세요.

            [내용 규칙]

            - user 프롬프트에 주어진 배경지식만 사용하세요. 당신이 알고 있는 법 개념을 추가하지 마세요.

            - 법 조항 번호, 판례 번호를 쓰지 마세요.

            - 관점은 항상 "보증금을 걱정하는 세입자"입니다. 매수인 관점으로 쓰지 마세요.

            - "집주인" 대신 "등기부상 집주인"이라고 쓰세요. 등기부에 적힌 사람이 실제 임대인이 아닐 수 있습니다.

            [국적 비교 규칙]

            - 모든 설명에 세입자 국적을 반드시 한 번 언급하세요. 위치는 첫 문장 또는 두 번째 문장.
            - 그 나라에 비슷한 제도가 있으면 → 이름을 대고 짧게 비교하세요.
              예: "베트남의 kê biên tài sản과 비슷한 개념이에요."
            - 비슷한 제도가 없거나 확실하지 않으면 → 없다고 명확하게 쓰세요.
              예: "베트남에는 이런 제도가 없어서 낯설 수 있어요."
              "잘 모르겠다"고 얼버무리지 말고, 없다고 명확히 쓰세요.
            - 제도 이름을 댈 때는 실제로 존재하는 것만 쓰세요.
              이름이 확실하지 않으면 이름 없이 "비슷한 제도가 있어요" 정도로만 쓰거나, 차라리 "없어요"라고 쓰세요.
            - 그 나라 법 조항 번호는 절대 쓰지 마세요.
            - 국적이 "알 수 없음"이면 국적 언급을 생략하세요.

            [한국 특유 제도 안내]

            아래 항목은 한국 외에는 거의 존재하지 않아요. 이 항목을 설명할 때는
            "○○(국가)에는 없는 한국만의 제도예요" 취지를 반드시 넣으세요.
            - 전세권 (JEONSE_RIGHT)
            - 임차권등기명령 (LEASE_REGISTRATION)

            아래 항목은 대부분의 나라에 유사 제도가 있어요. 비교가 가능하면 하세요.
            - 압류 (SEIZURE), 가압류 (PROVISIONAL_SEIZURE), 경매개시결정 (AUCTION_START)
            - 가등기 (PROVISIONAL_REGISTRATION), 신탁 (TRUST)

            [문체 규칙]

            - 한국어로 씁니다.

            - 5~6문장. 순서: 이게 무슨 뜻인지 → 국적 비교 → 내 보증금에 왜 위험한지 → 무엇을 해야 하는지

            - 문장은 짧게. 한 문장에 한 가지 내용만.

            - "~예요", "~해요" 체를 씁니다. "~합니다", "~됩니다" 금지.

            - 아래 목록에 있는 단어를 쓸 때는 반드시 괄호로 풀이를 붙이세요.
              한 설명 안에서 같은 단어를 두 번 이상 쓸 때는 첫 번째만 풀이하세요.
              예: 공매(세금을 안 내서 나라가 집을 강제로 파는 것)

              풀이 목록:
              "공매" → "세금을 안 내서 나라가 집을 강제로 파는 것"
              "경매" → "빚 때문에 법원이 집을 강제로 파는 것"
              "낙찰" → "경매에서 집을 사 가는 사람이 정해지는 것"
              "체납" → "세금을 내야 하는데 안 낸 것"
              "채권자" → "돈을 받아야 하는 사람이나 회사"
              "처분" → "집을 팔거나 남에게 넘기는 것"
              "설정" → "등기부에 권리를 등록하는 것"
              "본등기" → "정식으로 소유권을 넘기는 등기"
              "특약" → "계약서에 따로 넣는 약속"
              "잔금" → "계약금을 뺀 나머지 돈. 보통 이사하는 날 냄"
              "신탁회사" → "집의 소유권을 대신 맡아 관리하는 회사"
              "순위보전" → "먼저 등록한 사람이 먼저 돈을 받는 순서"
              "대항력" → "새 집주인에게도 계속 살 권리를 주장할 수 있는 힘"
              "우선변제권" → "다른 사람보다 먼저 보증금을 돌려받을 권리"
              "말소" → "지우는 것"
              "물권" → 굳이 쓰지 말 것

            - 겁주지 마세요. "위험합니다", "사기입니다" 대신 "~할 수 있어요" 톤.

            - 마지막 문장에는 반드시 다음 중 하나 이상이 들어가야 해요:
              (1) 중개인이나 집주인에게 물어볼 구체적인 질문
              (2) 계약서에 넣을 구체적인 약속 내용

              절대 이렇게 끝내지 마세요: "확인해보세요", "주의하세요", "알아보세요", "상담하세요"
              무엇을 확인하는지, 어떤 약속을 넣는지 구체적으로 쓰세요.

            [예시] — 아래는 모두 세입자 국적이 베트남인 경우 기준

            입력: 가등기 (국적: 베트남)

            출력: "집이 법적으로 다른 사람에게 넘어갈 예정이라, 등기부상 집주인이 바뀔 수 있다는 의미예요.
                  베트남에도 비슷한 부동산 선순위 등록 제도가 있어요.
                  집주인이 바뀌면 내 보증금을 누가 돌려줘야 하는지 불분명해질 수 있어요.
                  계약 전에 왜 가등기가 등록되었는지, 소유권이 어떻게 될 것인지 중개인에게 물어보세요.
                  직거래는 하지 말고, 중개인을 통해 소유권 이전 여부를 먼저 확인하세요."

            입력: 임차권등기명령 (국적: 베트남)

            출력: "앞서 이 집을 빌린 세입자가 보증금을 돌려받지 못한 기록이에요.
                  베트남에는 없는 한국만의 제도라 낯설 수 있어요.
                  돈을 못 받은 세입자가 이사한 뒤에도 보증금을 돌려받을 권리를 지키려고 법원에 요청해서 남긴 표시예요.
                  같은 등기부상 집주인에게 나도 같은 일을 당할 수 있다는 신호예요.
                  어떤 일이 있었는지, 그 세입자와 문제가 해결되었는지 중개인에게 구체적으로 물어보세요."

            입력: 압류 (국적: 베트남)

            출력: "등기부상 집주인이 세금을 내지 않아서(체납), 나라가 이 집을 팔지 못하게 막아둔 상태예요.
                  베트남의 kê biên tài sản과 비슷한 개념이에요.
                  집주인이 계속 세금을 안 내면 나라가 집을 강제로 팔 수 있어요(공매(세금을 안 내서 나라가 집을 강제로 파는 것)).
                  그렇게 되면 집을 판 돈에서 나라가 먼저 세금을 가져가서, 내 보증금은 못 받거나 일부만 받을 수 있어요.
                  세금이 얼마나 밀렸는지, 언제까지 압류가 풀리는지 중개인에게 물어보세요.
                  잔금(계약금을 뺀 나머지 돈. 보통 이사하는 날 냄) 내기 전에 압류를 없앤다는 약속을 계약서에 특약(계약서에 따로 넣는 약속)으로 넣어달라고 하세요."

            입력: 전세권 (국적: 베트남)

            출력: "앞에 살던 세입자가 '내 보증금을 먼저 돌려받겠다'고 등기부에 등록(설정(등기부에 권리를 등록하는 것))해둔 거예요.
                  베트남에는 없는 한국만의 제도라 낯설 수 있어요.
                  이게 남아 있으면, 나중에 집이 팔릴 때 그 사람이 나보다 먼저 돈을 받아가요.
                  그만큼 내 보증금이 뒤로 밀려요.
                  계약하기 전에 전세권을 지워달라고(말소(지우는 것)) 요구하세요.
                  '잔금 내기 전까지 전세권을 없애지 않으면 계약을 취소하고 계약금을 돌려받는다'는 약속을 계약서에 넣으세요."

            위 예시의 문장 길이와 말투를 따르세요. 내용은 배경지식에 있는 것만 쓰세요.

            주어진 JSON 스키마 형식으로만 응답하세요.
            """;

    private static final String JSON_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "explanations": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "riskType":    { "type": "string" },
                      "term":        { "type": "string" },
                      "explanation": { "type": "string" }
                    },
                    "required": ["riskType", "term", "explanation"],
                    "additionalProperties": false
                  }
                }
              },
              "required": ["explanations"],
              "additionalProperties": false
            }
            """;

    private final LlmJsonExtractor llmJsonExtractor;

    public List<RiskExplanation> explain(List<RegistryRiskType> detectedRisks, Nationality nationality) {
        if (detectedRisks.isEmpty()) {
            return List.of();
        }
        try {
            RiskExplanationsWrapper wrapper = llmJsonExtractor.extract(
                    SYSTEM_PROMPT, buildUserPrompt(detectedRisks, nationality),
                    SCHEMA_NAME, JSON_SCHEMA, RiskExplanationsWrapper.class);
            return wrapper.explanations();
        } catch (LlmExtractionException e) {
            log.warn("[RiskExplanationPipeline] 외국인 설명 생성 실패, 빈 리스트 반환: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildUserPrompt(List<RegistryRiskType> risks, Nationality nationality) {
        StringBuilder sb = new StringBuilder();
        sb.append("세입자 국적: ").append(nationality != null ? nationality.getLabel() : "알 수 없음").append("\n\n");
        sb.append("다음 위험 항목이 등기부등본에서 발견되었어요. 아래 배경지식만 사용해서 설명해주세요.\n\n");
        sb.append("=== 배경지식 ===\n");
        for (RegistryRiskType risk : risks) {
            sb.append(buildContext(risk)).append("\n");
        }
        sb.append("\n=== 설명할 항목 ===\n");
        sb.append("각 항목의 riskType 필드에는 괄호 안에 표기된 enum 이름을 정확히 입력하세요 (예: SEIZURE).\n");
        for (RegistryRiskType risk : risks) {
            sb.append("- ").append(risk.getLabel())
              .append(" (riskType: ").append(risk.name()).append(")\n");
        }
        return sb.toString();
    }

    private String buildContext(RegistryRiskType risk) {
        return switch (risk) {
            case PROVISIONAL_REGISTRATION ->
                    "[가등기 / PROVISIONAL_REGISTRATION]\n" +
                    "가등기는 장래에 소유권을 이전받을 권리를 미리 순위 보전해 둔 등기다. " +
                    "본등기가 완료되면 가등기 이후에 설정된 모든 권리(세입자의 대항력 포함)는 순위에서 밀려나 소멸할 수 있다. " +
                    "즉, 세입자가 계약 후 집주인이 바뀌거나 보증금 반환 의무 자체가 사라질 수 있다.";
            case TRUST ->
                    "[신탁 / TRUST]\n" +
                    "신탁이 설정된 경우 등기부상 소유자가 실제 처분권자가 아니다. 소유권이 신탁회사로 이전된 상태다. " +
                    "신탁회사의 동의 없이 위탁자(등기부상 전 소유자)와 임대차 계약을 체결하면 계약 자체가 무효가 될 수 있고, " +
                    "보증금을 반환받지 못할 수 있다.";
            case SEIZURE ->
                    "[압류 / SEIZURE]\n" +
                    "압류는 국세·지방세 체납 등으로 국가(세무서·지자체)가 해당 부동산의 처분을 금지한 상태다. " +
                    "집주인이 집에서 쫓겨나는 것은 아니지만, 체납이 해소되지 않으면 국가가 공매 절차를 진행할 수 있다. " +
                    "공매 낙찰 시 후순위 세입자는 보증금을 전액 돌려받지 못할 수 있다.";
            case PROVISIONAL_SEIZURE ->
                    "[가압류 / PROVISIONAL_SEIZURE]\n" +
                    "가압류는 민간 채권자가 소송 전에 재산 처분을 막아둔 상태다. 압류와 달리 국가가 아닌 개인·회사가 신청한다. " +
                    "소송 결과에 따라 경매로 이어질 수 있으며, 이 경우 세입자의 보증금 반환 순위가 밀릴 수 있다.";
            case AUCTION_START ->
                    "[경매개시결정 / AUCTION_START]\n" +
                    "경매개시결정은 이미 법원이 해당 부동산의 경매 절차를 공식 개시한 상태다. " +
                    "낙찰이 이루어지면 소유권이 낙찰자에게 넘어가며, 후순위 세입자는 보증금을 전부 또는 일부 잃을 수 있다. " +
                    "7가지 위험 항목 중 가장 위중한 상태로 본다.";
            case LEASE_REGISTRATION ->
                    "[임차권등기명령 / LEASE_REGISTRATION]\n" +
                    "임차권등기명령은 이전 세입자가 계약 종료 후 보증금을 돌려받지 못해 법원에 신청한 등기다(주택임대차보호법 제3조의3). " +
                    "이사한 후에도 대항력·우선변제권을 유지하기 위한 제도적 수단이다. " +
                    "이 등기가 남아 있다는 것은 집주인이 과거에 보증금을 제때 반환하지 못한 이력이 있다는 신호다.";
            case JEONSE_RIGHT ->
                    "[전세권 / JEONSE_RIGHT]\n" +
                    "전세권은 이전 세입자가 설정했을 수 있는 물권으로, 월세 계약에서도 설정될 수 있다. " +
                    "전세권이 말소되지 않은 상태에서 새 계약을 체결하면 해당 전세권자가 경매 시 우선변제를 받아 " +
                    "새 세입자의 보증금 회수 순위가 밀릴 수 있다.";
        };
    }

    // LlmJsonExtractor 파싱용 내부 래퍼
    @JsonIgnoreProperties(ignoreUnknown = true)
    record RiskExplanationsWrapper(@NotNull List<@Valid RiskExplanation> explanations) {}
}