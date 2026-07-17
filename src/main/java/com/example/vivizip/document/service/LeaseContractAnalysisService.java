package com.example.vivizip.document.service;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.document.dto.임대차계약서.*;
import com.example.vivizip.document.dto.중개대상물.BoundingBox;
import com.example.vivizip.document.dto.중개대상물.HighlightRegion;
import com.example.vivizip.document.entity.ReferenceBaseline;
import com.example.vivizip.document.pipeline.LeaseContractValuePipeline;
import com.example.vivizip.document.pipeline.MismatchMessagePipeline;
import com.example.vivizip.document.pipeline.RiskyClausePipeline;
import com.example.vivizip.document.repository.LeaseCaseRepository;
import com.example.vivizip.document.repository.ReferenceBaselineRepository;
import com.example.vivizip.ocr.dto.KeywordSearchResponse;
import com.example.vivizip.ocr.dto.OcrSaveResponse;
import com.example.vivizip.ocr.service.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// 임대차계약서 분석: OCR → AI 값 추출 → 하이라이트 박스 → reference_baseline 비교 → 불일치 멘트 → 위험 특약 → 응답 조립.
// 중개대상물과 동일하게 1회성 분석이며 DB에 분석 결과를 저장하지 않는다.
// reference_baseline은 읽기 전용으로만 사용한다 (업데이트 없음).
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaseContractAnalysisService {

    private final LeaseCaseRepository leaseCaseRepository;
    private final ReferenceBaselineRepository referenceBaselineRepository;
    private final OcrService ocrService;
    private final LeaseContractValuePipeline leaseContractValuePipeline;
    private final MismatchMessagePipeline mismatchMessagePipeline;
    private final RiskyClausePipeline riskyClausePipeline;

    public LeaseContractAnalysisResponse analyze(Long userId, Long leaseCaseId, List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new GeneralException(ErrorStatus.DOCUMENT_FILE_EMPTY);
        }
        leaseCaseRepository.findByIdAndUserId(leaseCaseId, userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LEASE_CASE_NOT_FOUND));

        // 1. OCR 저장 (트랜잭션 밖에서 CLOVA 호출 후 저장)
        OcrSaveResponse saved = ocrService.save(userId, files);

        // 2. 텍스트 추출 (저장된 rawJson 재활용)
        String ocrText = ocrService.getText(userId, saved.id());

        // 3. AI 값 추출 (LLM 호출 — 트랜잭션 밖)
        LeaseContractExtractedValues extracted = leaseContractValuePipeline.extract(ocrText);

        // 4. 하이라이트 박스 — 라벨이 아닌 추출된 값으로 검색
        // 임대인: 서명란이 하단에 있으므로 마지막 매치 사용
        List<HighlightRegion> ownerRegions        = searchLastMatch(userId, saved.id(), extracted.owner(), "임대인");
        // 계약일: 원문 그대로 검색
        List<HighlightRegion> contractDateRegions = searchFirstMatch(userId, saved.id(), extracted.contractDateRawText(), "계약일");
        // 소재지: 폴백 검색 (전체 → 앞 10글자)
        List<HighlightRegion> addressRegions      = searchAddressFallback(userId, saved.id(), extracted.roadAddressRawText(), "소재지");
        // 임대차 기간 시작·종료일: 원문 그대로 검색
        List<HighlightRegion> leaseStartRegions   = searchFirstMatch(userId, saved.id(), extracted.leaseStartDateRawText(), "임대차 시작일");
        List<HighlightRegion> leaseEndRegions     = searchFirstMatch(userId, saved.id(), extracted.leaseEndDateRawText(), "임대차 종료일");
        // 보증금·차임: 원문 그대로 검색
        List<HighlightRegion> depositRegions      = searchFirstMatch(userId, saved.id(), extracted.depositRawText(), "보증금");
        List<HighlightRegion> rentRegions         = searchFirstMatch(userId, saved.id(), extracted.monthlyRentRawText(), "차임");

        List<HighlightRegion> basicInfoRegions = new ArrayList<>();
        basicInfoRegions.addAll(ownerRegions);
        basicInfoRegions.addAll(contractDateRegions);
        basicInfoRegions.addAll(addressRegions);
        basicInfoRegions.addAll(leaseStartRegions);
        basicInfoRegions.addAll(leaseEndRegions);

        List<HighlightRegion> costRegions = new ArrayList<>(depositRegions);
        costRegions.addAll(rentRegions);

        // 5. reference_baseline 비교 (읽기 전용)
        Optional<ReferenceBaseline> baseline = referenceBaselineRepository.findByLeaseCaseId(leaseCaseId);

        Boolean ownerMatched       = compareOwner(extracted, baseline);
        Boolean addressMatched     = compareAddress(extracted, baseline);
        Boolean depositMatched     = compareDeposit(extracted, baseline);
        Boolean monthlyRentMatched = compareMonthlyRent(extracted, baseline);

        Boolean matchesBrokerageDocument = (ownerMatched == null || addressMatched == null)
                ? null
                : ownerMatched && addressMatched;

        // 6. 불일치 멘트 생성 (depositMatched 또는 monthlyRentMatched가 false일 때만 LLM 호출)
        String depositMessage     = null;
        String monthlyRentMessage = null;
        if (Boolean.FALSE.equals(depositMatched) || Boolean.FALSE.equals(monthlyRentMatched)) {
            MismatchMessages messages = mismatchMessagePipeline.generate(
                    Boolean.FALSE.equals(depositMatched),
                    baseline.map(ReferenceBaseline::getDeposit).orElse(null),
                    extracted.deposit(),
                    Boolean.FALSE.equals(monthlyRentMatched),
                    baseline.map(ReferenceBaseline::getMonthlyRent).orElse(null),
                    extracted.monthlyRent());
            if (Boolean.FALSE.equals(depositMatched))     depositMessage     = messages.depositMessage();
            if (Boolean.FALSE.equals(monthlyRentMatched)) monthlyRentMessage = messages.monthlyRentMessage();
        }

        // 7. 위험 특약 분석 (specialClauses가 비어 있으면 스킵)
        List<RiskyClauseResult> riskyClauses = List.of();
        List<String> specialClauses = extracted.specialClauses();
        if (specialClauses != null && !specialClauses.isEmpty()) {
            List<RiskyClauseRaw> rawClauses = riskyClausePipeline.analyze(specialClauses);
            riskyClauses = buildRiskyClauseResults(userId, saved.id(), rawClauses);
        }

        // 8. 응답 조립
        LeaseContractBasicInfoResult basicInfo = new LeaseContractBasicInfoResult(
                matchesBrokerageDocument,
                extracted.owner(),
                extracted.contractDate(),
                extracted.roadAddress(),
                extracted.leaseStartDate(),
                extracted.leaseEndDate(),
                basicInfoRegions);

        LeaseContractCostResult cost = new LeaseContractCostResult(
                extracted.deposit(),
                extracted.monthlyRent(),
                depositMatched,
                monthlyRentMatched,
                depositMessage,
                monthlyRentMessage,
                costRegions);

        return new LeaseContractAnalysisResponse(basicInfo, cost, riskyClauses);
    }

    // --- 비교 로직 ---

    private Boolean compareOwner(LeaseContractExtractedValues extracted, Optional<ReferenceBaseline> baseline) {
        return baseline
                .map(ReferenceBaseline::getBrokerageDocumentOwnerName)
                .filter(Objects::nonNull)
                // 공동명의 cross-match: 양쪽 이름 목록(쉼표 구분)에서 하나라도 교집합이 있으면 true
                .map(brokerageOwner -> anyOwnerMatches(extracted.owner(), brokerageOwner))
                .orElse(null);
    }

    private Boolean compareAddress(LeaseContractExtractedValues extracted, Optional<ReferenceBaseline> baseline) {
        return baseline
                .map(ReferenceBaseline::getBrokerageDocumentAddress)
                .filter(Objects::nonNull)
                .map(brokerageAddr -> {
                    String extractedCore = normalizeAddressCore(extracted.roadAddress());
                    String baselineCore  = normalizeAddressCore(brokerageAddr);
                    return !extractedCore.isEmpty() && !baselineCore.isEmpty()
                            && (extractedCore.contains(baselineCore) || baselineCore.contains(extractedCore));
                })
                .orElse(null);
    }

    private Boolean compareDeposit(LeaseContractExtractedValues extracted, Optional<ReferenceBaseline> baseline) {
        return baseline
                .map(ReferenceBaseline::getDeposit)
                .filter(Objects::nonNull)
                .map(brokerageDeposit -> Objects.equals(brokerageDeposit, extracted.deposit()))
                .orElse(null);
    }

    private Boolean compareMonthlyRent(LeaseContractExtractedValues extracted, Optional<ReferenceBaseline> baseline) {
        return baseline
                .map(ReferenceBaseline::getMonthlyRent)
                .filter(Objects::nonNull)
                .map(brokerageRent -> Objects.equals(brokerageRent, extracted.monthlyRent()))
                .orElse(null);
    }

    // --- 하이라이트 박스 검색 ---

    // 임대인 이름 전용: 서명란이 하단에 있으므로 마지막 매치 사용
    private List<HighlightRegion> searchLastMatch(Long userId, Long ocrResultId, String value, String label) {
        if (value == null || value.isBlank()) return List.of();
        try {
            KeywordSearchResponse response = ocrService.searchKeywordByField(userId, ocrResultId, value);
            List<KeywordSearchResponse.MatchResult> matches = response.matches();
            if (matches.isEmpty()) {
                log.warn("[LeaseContract] 좌표 검색 실패 — label={}, keyword={}", label, value);
                return List.of();
            }
            return toRegion(matches.get(matches.size() - 1), label).map(List::of).orElse(List.of());
        } catch (Exception e) {
            log.warn("[LeaseContract] 좌표 검색 오류 — label={}, error={}", label, e.getMessage());
            return List.of();
        }
    }

    // 일반 검색: 첫 번째 매치
    private List<HighlightRegion> searchFirstMatch(Long userId, Long ocrResultId, String value, String label) {
        if (value == null || value.isBlank()) return List.of();
        try {
            KeywordSearchResponse response = ocrService.searchKeywordByField(userId, ocrResultId, value);
            List<KeywordSearchResponse.MatchResult> matches = response.matches();
            if (matches.isEmpty()) {
                log.warn("[LeaseContract] 좌표 검색 실패 — label={}, keyword={}", label, value);
                return List.of();
            }
            return toRegion(matches.get(0), label).map(List::of).orElse(List.of());
        } catch (Exception e) {
            log.warn("[LeaseContract] 좌표 검색 오류 — label={}, error={}", label, e.getMessage());
            return List.of();
        }
    }

    // 소재지 전용: 전체 → 앞 10글자 폴백
    private List<HighlightRegion> searchAddressFallback(Long userId, Long ocrResultId, String rawText, String label) {
        if (rawText == null || rawText.isBlank()) return List.of();
        List<HighlightRegion> regions = searchFirstMatch(userId, ocrResultId, rawText, label);
        if (!regions.isEmpty()) return regions;

        if (rawText.length() > 10) {
            regions = searchFirstMatch(userId, ocrResultId, rawText.substring(0, 10), label);
        }
        if (regions.isEmpty()) {
            log.warn("[LeaseContract] 소재지 좌표 검색 최종 실패 — rawText={}", rawText);
        }
        return regions;
    }

    private Optional<HighlightRegion> toRegion(KeywordSearchResponse.MatchResult match, String label) {
        List<KeywordSearchResponse.Vertex> vertices = match.vertices();
        if (vertices == null || vertices.size() < 3) return Optional.empty();

        KeywordSearchResponse.Vertex topLeft     = vertices.get(0);
        KeywordSearchResponse.Vertex bottomRight = vertices.get(2);
        BoundingBox box = new BoundingBox(
                topLeft.x(), topLeft.y(),
                bottomRight.x() - topLeft.x(), bottomRight.y() - topLeft.y());
        return Optional.of(new HighlightRegion(match.pageIndex(), box, label));
    }

    // --- 위험 특약 좌표 검색 (폴백: 전체 → 앞 10글자 → 빈 배열) ---

    private List<RiskyClauseResult> buildRiskyClauseResults(Long userId, Long ocrResultId,
                                                              List<RiskyClauseRaw> rawClauses) {
        List<RiskyClauseResult> results = new ArrayList<>();
        for (RiskyClauseRaw raw : rawClauses) {
            List<HighlightRegion> regions = searchRiskyClauseRegions(userId, ocrResultId, raw.originalText());
            results.add(new RiskyClauseResult(raw.originalText(), raw.reason(), raw.suggestion(), regions));
        }
        return results;
    }

    private List<HighlightRegion> searchRiskyClauseRegions(Long userId, Long ocrResultId, String originalText) {
        List<HighlightRegion> regions = searchFirstMatch(userId, ocrResultId, originalText, "특약");
        if (!regions.isEmpty()) return regions;

        if (originalText != null && originalText.length() > 10) {
            regions = searchFirstMatch(userId, ocrResultId, originalText.substring(0, 10), "특약");
        }
        return regions;
    }

    // --- 문자열 정규화 ---

    // 공동명의 cross-match: 양쪽 이름 목록(쉼표 구분)에서 하나라도 교집합이 있으면 true
    private boolean anyOwnerMatches(String names1, String names2) {
        if (names1 == null || names2 == null) return false;
        java.util.Set<String> set = java.util.Arrays.stream(names1.split(","))
                .map(this::normalize).collect(java.util.stream.Collectors.toSet());
        return java.util.Arrays.stream(names2.split(","))
                .map(this::normalize).anyMatch(set::contains);
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "");
    }

    private String normalizeAddressCore(String value) {
        String v = normalize(value);
        v = v.replaceAll("외\\d+필지$", "");
        v = v.replaceAll("\\d+동\\d+호$", "");
        v = v.replaceAll("\\d+호$", "");
        return v;
    }
}