package com.example.vivizip.document.service;

import com.example.vivizip.S3.enums.S3Folder;
import com.example.vivizip.S3.service.S3Service;
import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.document.dto.임대차계약서.*;
import com.example.vivizip.document.dto.중개대상물.BoundingBox;
import com.example.vivizip.document.dto.중개대상물.HighlightRegion;
import com.example.vivizip.document.entity.AnalysisType;
import com.example.vivizip.document.entity.LeaseCase;
import com.example.vivizip.document.entity.LeaseDocument;
import com.example.vivizip.document.entity.LeaseDocumentType;
import com.example.vivizip.document.entity.ReferenceBaseline;
import com.example.vivizip.document.pipeline.LeaseContractValuePipeline;
import com.example.vivizip.document.pipeline.MismatchMessagePipeline;
import com.example.vivizip.document.pipeline.RiskyClausePipeline;
import com.example.vivizip.document.repository.LeaseCaseRepository;
import com.example.vivizip.document.repository.ReferenceBaselineRepository;
import com.example.vivizip.ocr.dto.KeywordSearchResponse;
import com.example.vivizip.ocr.dto.OcrSaveResponse;
import com.example.vivizip.ocr.service.OcrService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// 임대차계약서 분석: 업로드(S3) → OCR → AI 값 추출 → 하이라이트 박스 → reference_baseline 비교 → 불일치 멘트 → 위험 특약 → 응답 조립.
// 분석 상태·결과는 document_analysis 테이블에 저장한다.
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
    private final S3Service s3Service;
    private final LeaseDocumentUploadRecorder leaseDocumentUploadRecorder;
    private final DocumentAnalysisRecorder documentAnalysisRecorder;
    private final ObjectMapper objectMapper;

    public LeaseContractAnalysisResponse analyze(Long userId, Long leaseCaseId, List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new GeneralException(ErrorStatus.DOCUMENT_FILE_EMPTY);
        }
        leaseCaseRepository.findByIdAndUserId(leaseCaseId, userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LEASE_CASE_NOT_FOUND));

        LeaseDocument document = uploadFiles(leaseCaseId, files);
        Long analysisId = documentAnalysisRecorder.start(document.getId(), AnalysisType.LEASE_CONTRACT_ANALYSIS);

        try {
            LeaseContractAnalysisResponse result = doAnalyze(userId, leaseCaseId, files);
            documentAnalysisRecorder.complete(document.getId(), analysisId, AnalysisType.LEASE_CONTRACT_ANALYSIS, null, toJson(result));
            return result;
        } catch (IOException | RuntimeException e) {
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            documentAnalysisRecorder.fail(document.getId(), analysisId, reason);
            throw e;
        }
    }

    private LeaseDocument uploadFiles(Long leaseCaseId, List<MultipartFile> files) {
        List<String> uploadedKeys = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                uploadedKeys.add(s3Service.uploadPrivate(file, S3Folder.LEASE_DOCUMENT.getPath()));
            }
            return leaseDocumentUploadRecorder.saveUploadedDocument(leaseCaseId, LeaseDocumentType.LEASE_CONTRACT, uploadedKeys);
        } catch (RuntimeException e) {
            uploadedKeys.forEach(s3Service::delete);
            throw e;
        }
    }

    private LeaseContractAnalysisResponse doAnalyze(Long userId, Long leaseCaseId, List<MultipartFile> files) throws IOException {
        LeaseCase leaseCase = leaseCaseRepository.findByIdAndUserId(leaseCaseId, userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LEASE_CASE_NOT_FOUND));

        // 1. OCR 저장 (트랜잭션 밖에서 CLOVA 호출 후 저장)
        OcrSaveResponse saved = ocrService.save(userId, files);

        // 2. 텍스트 추출 (저장된 rawJson 재활용)
        String ocrText = ocrService.getText(userId, saved.id());

        // 3. AI 값 추출 (LLM 호출 — 트랜잭션 밖)
        LeaseContractExtractedValues extracted = leaseContractValuePipeline.extract(ocrText);

        // 4. 하이라이트 박스
        // [basicInfo.regions] 값 좌표: 제2조 라인 안의 임대차 시작일·종료일 날짜 텍스트
        // 다중 매치 시 제2조 y축에 가장 근접한 것 선택 (계약금 지급일 등 다른 날짜와 구별)
        List<HighlightRegion> leaseStartRegions = searchDateOnArticle2Line(userId, saved.id(), extracted.leaseStartDateRawText(), "임대차 시작일");
        List<HighlightRegion> leaseEndRegions   = searchDateOnArticle2Line(userId, saved.id(), extracted.leaseEndDateRawText(), "임대차 종료일");

        List<HighlightRegion> basicInfoRegions = new ArrayList<>();
        basicInfoRegions.addAll(leaseStartRegions);
        basicInfoRegions.addAll(leaseEndRegions);

        // [cost.regions] 라벨 좌표: 항목 제목 텍스트
        // 매칭 실패 시 해당 region은 자동 생략
        List<HighlightRegion> costRegions = new ArrayList<>();
        // 임대인: 서명 표 블록 안에서 검색 (세로쓰기 대응 + 앵커 필터)
        costRegions.addAll(searchOwnerInSignatureBlock(userId, saved.id()));
        costRegions.addAll(searchByLabel(userId, saved.id(), "소재지"));
        costRegions.addAll(searchByLabel(userId, saved.id(), "보증금"));
        costRegions.addAll(searchByLabel(userId, saved.id(), "계약금"));
        // 차임/월세: "2. 계약내용" 표 라벨 열 한정 검색 (부분 매칭·오인식 방지)
        costRegions.addAll(searchRentLabelInContractTable(userId, saved.id()));

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
        // 특약사항 좌표는 문서의 "특약사항" 라벨 위치 1개를 모든 항목이 공유
        List<RiskyClauseResult> riskyClauses = List.of();
        List<String> specialClauses = extracted.specialClauses();
        if (specialClauses != null && !specialClauses.isEmpty()) {
            List<HighlightRegion> specialClauseRegions = searchByLabel(userId, saved.id(), "특약사항");
            List<RiskyClauseRaw> rawClauses = riskyClausePipeline.analyze(specialClauses);
            riskyClauses = rawClauses.stream()
                    .map(raw -> new RiskyClauseResult(raw.originalText(), raw.reason(), raw.suggestion(), specialClauseRegions))
                    .toList();
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

        // 9. 계약서 검토(부메랑 3단계) 완료 처리 — 여기까지 예외 없이 왔으면 이 케이스의 계약서 검토가 끝난 것으로 본다.
        leaseCase.complete();
        leaseCaseRepository.save(leaseCase);

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

    // 서명 표 블록 안의 "임대인" 라벨 검색
    // 세로쓰기("임","대","인") + 단일 field "임대인" 모두 후보에 포함
    // "본 계약을 증명하기" 앵커 아래, 서명 블록에서 y 최소(= 최상단 = 임대인 행) 선택
    private List<HighlightRegion> searchOwnerInSignatureBlock(Long userId, Long ocrResultId) {
        try {
            // 1. 앵커 y: "본 계약을 증명하기" 문장 위치
            double anchorY = findAnchorY(userId, ocrResultId, "본 계약을 증명하기");

            // 2. 단일 field "임대인" 후보
            List<KeywordSearchResponse.MatchResult> singleMatches =
                    ocrService.searchKeywordByField(userId, ocrResultId, "임대인").matches();

            // 3. 세로쓰기 ["임","대","인"] 후보 (x 허용 오차 30px)
            List<KeywordSearchResponse.MatchResult> verticalMatches =
                    ocrService.searchVerticalChars(userId, ocrResultId, List.of("임", "대", "인"), 30.0).matches();

            // 4. 합산 후 앵커 아래(서명 블록 내) 필터
            List<KeywordSearchResponse.MatchResult> inBlock = new ArrayList<>();
            inBlock.addAll(singleMatches);
            inBlock.addAll(verticalMatches);
            if (anchorY > 0) {
                inBlock = inBlock.stream()
                        .filter(m -> centerY(m.vertices()) > anchorY)
                        .collect(Collectors.toList());
            }

            if (inBlock.isEmpty()) {
                log.warn("[LeaseContract] 임대인 서명 블록 내 좌표 검색 실패");
                return List.of();
            }

            // 5. 서명 블록 최상단(y 최소) → 임대인 행 (임차인·공동명의인보다 위)
            KeywordSearchResponse.MatchResult best = inBlock.stream()
                    .min(Comparator.comparingDouble(m -> centerY(m.vertices())))
                    .orElse(inBlock.get(0));

            return toRegion(best, "임대인").map(List::of).orElse(List.of());
        } catch (Exception e) {
            log.warn("[LeaseContract] 임대인 좌표 검색 오류 — error={}", e.getMessage());
            return List.of();
        }
    }

    // "2. 계약내용" 표 라벨 열에서 "차임" 또는 "월세" 라벨 검색
    // 보증금 x 기준 ±50px + 제1조~제2조 y 범위 필터 + 정확 일치
    private List<HighlightRegion> searchRentLabelInContractTable(Long userId, Long ocrResultId) {
        try {
            // 보증금 라벨의 x 중심 (라벨 열 기준점)
            double depositX = findLabelCenterX(userId, ocrResultId, "보증금");
            // y 범위 앵커
            double article1Y = findAnchorY(userId, ocrResultId, "제1조");
            double article2Y = findAnchorY(userId, ocrResultId, "제2조");

            // 정확 일치 후보 ("차임", "월세")
            List<KeywordSearchResponse.MatchResult> candidates = new ArrayList<>();
            candidates.addAll(ocrService.searchExactKeyword(userId, ocrResultId, "차임").matches());
            candidates.addAll(ocrService.searchExactKeyword(userId, ocrResultId, "월세").matches());

            if (candidates.isEmpty()) {
                log.warn("[LeaseContract] 차임/월세 후보 없음");
                return List.of();
            }

            // x 기준 ±50px + 제1조 아래 + 제2조 위
            List<KeywordSearchResponse.MatchResult> filtered = candidates.stream()
                    .filter(m -> {
                        double cx = centerX(m.vertices());
                        double cy = centerY(m.vertices());
                        boolean xOk = depositX <= 0 || Math.abs(cx - depositX) <= 50.0;
                        boolean belowArticle1 = article1Y <= 0 || cy > article1Y;
                        boolean aboveArticle2 = article2Y <= 0 || cy < article2Y;
                        return xOk && belowArticle1 && aboveArticle2;
                    })
                    .collect(Collectors.toList());

            if (filtered.isEmpty()) {
                log.warn("[LeaseContract] 차임/월세 필터 후 후보 없음");
                return List.of();
            }

            // 범위 내 y 최대 (= 라벨 열 최하단 = 차임 행)
            KeywordSearchResponse.MatchResult best = filtered.stream()
                    .max(Comparator.comparingDouble(m -> centerY(m.vertices())))
                    .orElse(filtered.get(0));

            return toRegion(best, "차임").map(List::of).orElse(List.of());
        } catch (Exception e) {
            log.warn("[LeaseContract] 차임 좌표 검색 오류 — error={}", e.getMessage());
            return List.of();
        }
    }

    // 라벨 field의 x 중심 반환 (미발견 시 0.0)
    private double findLabelCenterX(Long userId, Long ocrResultId, String keyword) {
        try {
            return ocrService.searchKeywordByField(userId, ocrResultId, keyword).matches().stream()
                    .findFirst()
                    .map(m -> centerX(m.vertices()))
                    .orElse(0.0);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double centerX(List<KeywordSearchResponse.Vertex> vertices) {
        if (vertices == null || vertices.size() < 2) return 0.0;
        Double x0 = vertices.get(0).x(); // top-left (minX)
        Double x1 = vertices.get(1).x(); // top-right (maxX)
        if (x0 == null || x1 == null) return 0.0;
        return (x0 + x1) / 2.0;
    }

    // "본 계약을 증명하기" 등 앵커 문장의 y 중심 반환 (미발견 시 0.0)
    private double findAnchorY(Long userId, Long ocrResultId, String anchorText) {
        try {
            KeywordSearchResponse anchor = ocrService.searchKeyword(userId, ocrResultId, anchorText);
            return anchor.matches().stream()
                    .findFirst()
                    .map(m -> centerY(m.vertices()))
                    .orElse(0.0);
        } catch (Exception e) {
            return 0.0;
        }
    }

    // 문서에 적힌 라벨 키워드("소재지", "특약사항")로 검색 — 단어 단위 box, 첫 번째 매치
    private List<HighlightRegion> searchByLabel(Long userId, Long ocrResultId, String keyword) {
        try {
            KeywordSearchResponse response = ocrService.searchKeywordByField(userId, ocrResultId, keyword);
            List<KeywordSearchResponse.MatchResult> matches = response.matches();
            if (matches.isEmpty()) {
                log.warn("[LeaseContract] 라벨 좌표 검색 실패 — keyword={}", keyword);
                return List.of();
            }
            return toRegion(matches.get(0), keyword).map(List::of).orElse(List.of());
        } catch (Exception e) {
            log.warn("[LeaseContract] 라벨 좌표 검색 오류 — keyword={}, error={}", keyword, e.getMessage());
            return List.of();
        }
    }

    // 제2조 라인에서 날짜 rawText 검색 — 단어 단위 box, 다중 매치 시 제2조 y축에 가장 근접한 것 선택
    private List<HighlightRegion> searchDateOnArticle2Line(Long userId, Long ocrResultId, String rawText, String label) {
        if (rawText == null || rawText.isBlank()) return List.of();
        try {
            KeywordSearchResponse response = ocrService.searchKeywordByField(userId, ocrResultId, rawText);
            List<KeywordSearchResponse.MatchResult> matches = response.matches();
            if (matches.isEmpty()) {
                log.warn("[LeaseContract] 날짜 좌표 검색 실패 — label={}, rawText={}", label, rawText);
                return List.of();
            }
            KeywordSearchResponse.MatchResult best = matches.size() == 1
                    ? matches.get(0)
                    : pickNearestToArticle2(userId, ocrResultId, matches);
            return toRegion(best, label).map(List::of).orElse(List.of());
        } catch (Exception e) {
            log.warn("[LeaseContract] 날짜 좌표 검색 오류 — label={}, error={}", label, e.getMessage());
            return List.of();
        }
    }

    // 매치 목록 중 제2조 y축에 가장 근접한 것 반환 (제2조 미발견 시 첫 번째 매치)
    private KeywordSearchResponse.MatchResult pickNearestToArticle2(Long userId, Long ocrResultId,
                                                                      List<KeywordSearchResponse.MatchResult> matches) {
        try {
            KeywordSearchResponse article2 = ocrService.searchKeywordByField(userId, ocrResultId, "제2조");
            if (article2.matches().isEmpty()) return matches.get(0);
            double article2Y = centerY(article2.matches().get(0).vertices());
            return matches.stream()
                    .min(Comparator.comparingDouble(m -> Math.abs(centerY(m.vertices()) - article2Y)))
                    .orElse(matches.get(0));
        } catch (Exception e) {
            return matches.get(0);
        }
    }

    private double centerY(List<KeywordSearchResponse.Vertex> vertices) {
        if (vertices == null || vertices.size() < 3) return 0.0;
        Double y0 = vertices.get(0).y();
        Double y2 = vertices.get(2).y();
        if (y0 == null || y2 == null) return 0.0;
        return (y0 + y2) / 2.0;
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

    private String toJson(LeaseContractAnalysisResponse result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }

    // --- 문자열 정규화 ---

    // 공동명의 cross-match: 양쪽 이름 목록(쉼표 구분)에서 하나라도 교집합이 있으면 true
    private boolean anyOwnerMatches(String names1, String names2) {
        if (names1 == null || names2 == null) return false;
        Set<String> set = java.util.Arrays.stream(names1.split(","))
                .map(this::normalize).collect(Collectors.toSet());
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