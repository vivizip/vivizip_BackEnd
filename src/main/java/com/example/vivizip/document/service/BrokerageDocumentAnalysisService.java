package com.example.vivizip.document.service;

import com.example.vivizip.S3.enums.S3Folder;
import com.example.vivizip.S3.service.S3Service;
import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.document.dto.중개대상물.BasicInfoResult;
import com.example.vivizip.document.dto.중개대상물.BoundingBox;
import com.example.vivizip.document.dto.중개대상물.BrokerageDocumentAnalysisResponse;
import com.example.vivizip.document.dto.중개대상물.BrokerageDocumentExtractedValues;
import com.example.vivizip.document.dto.중개대상물.HighlightRegion;
import com.example.vivizip.document.dto.중개대상물.LiabilityResult;
import com.example.vivizip.document.dto.중개대상물.MortgageResult;
import com.example.vivizip.document.entity.AnalysisType;
import com.example.vivizip.document.entity.LeaseDocument;
import com.example.vivizip.document.entity.LeaseDocumentType;
import com.example.vivizip.document.entity.ReferenceBaseline;
import com.example.vivizip.document.pipeline.BrokerageDocumentValuePipeline;
import com.example.vivizip.document.repository.LeaseCaseRepository;
import com.example.vivizip.document.repository.ReferenceBaselineRepository;
import com.example.vivizip.ocr.dto.KeywordSearchResponse;
import com.example.vivizip.ocr.dto.OcrSaveResponse;
import com.example.vivizip.ocr.service.OcrService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// 중개대상물 확인·설명서 분석: 업로드(S3) → OCR → (값은 AI로 추출 / 박스는 기존 키워드 검색 서비스로 획득)
// → reference_baseline 비교/갱신 → 응답 조립. building-ledger/registry와 동일하게 lease_document,
// document_analysis에도 상태·결과를 기록한다.
@Service
@RequiredArgsConstructor
public class BrokerageDocumentAnalysisService {

    private static final String KEYWORD_ADDRESS = "소재지";
    private static final String KEYWORD_OWNER = "매도인";
    private static final String KEYWORD_MORTGAGE = "소유권 외의 권리사항";
    private static final String KEYWORD_LIABILITY = "손해배상책임";

    private final LeaseCaseRepository leaseCaseRepository;
    private final ReferenceBaselineRepository referenceBaselineRepository;
    private final OcrService ocrService;
    private final BrokerageDocumentValuePipeline brokerageDocumentValuePipeline;
    private final S3Service s3Service;
    private final LeaseDocumentUploadRecorder leaseDocumentUploadRecorder;
    private final DocumentAnalysisRecorder documentAnalysisRecorder;
    private final ObjectMapper objectMapper;

    public BrokerageDocumentAnalysisResponse analyze(Long userId, Long leaseCaseId, List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new GeneralException(ErrorStatus.DOCUMENT_FILE_EMPTY);
        }
        leaseCaseRepository.findByIdAndUserId(leaseCaseId, userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LEASE_CASE_NOT_FOUND));

        LeaseDocument document = uploadFiles(leaseCaseId, files);
        Long analysisId = documentAnalysisRecorder.start(document.getId(), AnalysisType.BROKERAGE_DOCUMENT_ANALYSIS);

        try {
            BrokerageDocumentAnalysisResponse result = doAnalyze(userId, leaseCaseId, files);
            documentAnalysisRecorder.complete(
                    document.getId(), analysisId, AnalysisType.BROKERAGE_DOCUMENT_ANALYSIS, result, toJson(result));
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
            return leaseDocumentUploadRecorder.saveUploadedDocument(leaseCaseId, LeaseDocumentType.BROKERAGE_CONFIRMATION, uploadedKeys);
        } catch (RuntimeException e) {
            uploadedKeys.forEach(s3Service::delete);
            throw e;
        }
    }

    private BrokerageDocumentAnalysisResponse doAnalyze(Long userId, Long leaseCaseId, List<MultipartFile> files) throws IOException {
        OcrSaveResponse saved = ocrService.save(userId, files);
        String ocrText = ocrService.getText(userId, saved.id());

        BrokerageDocumentExtractedValues extracted = brokerageDocumentValuePipeline.extract(ocrText);

        List<HighlightRegion> addressRegions = searchRegions(userId, saved.id(), KEYWORD_ADDRESS, true);
        List<HighlightRegion> ownerRegions = searchRegions(userId, saved.id(), KEYWORD_OWNER, false);
        List<HighlightRegion> mortgageRegions = searchRegions(userId, saved.id(), KEYWORD_MORTGAGE, false);
        List<HighlightRegion> liabilityRegions = searchRegions(userId, saved.id(), KEYWORD_LIABILITY, true);

        List<HighlightRegion> basicInfoRegions = new ArrayList<>(addressRegions);
        basicInfoRegions.addAll(ownerRegions);

        Optional<ReferenceBaseline> baseline = referenceBaselineRepository.findByLeaseCaseId(leaseCaseId);
        baseline.ifPresent(b -> {
            b.updateFromBrokerageDocument(extracted.owner(), extracted.roadAddress(), extracted.deposit(), extracted.monthlyRent());
            referenceBaselineRepository.save(b);
        });

        BasicInfoResult basicInfo = new BasicInfoResult(
                baseline.map(b -> matchesBasicInfo(extracted, b)).orElse(null),
                extracted.owner(),
                extracted.roadAddress(),
                basicInfoRegions);

        MortgageResult mortgage = new MortgageResult(
                baseline.map(b -> extracted.documentHasMortgage() == b.isHasMortgage()).orElse(null),
                mortgageRegions);

        LiabilityResult liability = new LiabilityResult(liabilityRegions);

        return new BrokerageDocumentAnalysisResponse(basicInfo, mortgage, liability, extracted.brokerageFee());
    }

    private boolean matchesBasicInfo(BrokerageDocumentExtractedValues extracted, ReferenceBaseline baseline) {
        boolean ownerMatches = normalize(extracted.owner()).equals(normalize(baseline.getOwnerName()));
        String extractedAddress = normalizeAddressCore(extracted.roadAddress());
        String baselineAddress = normalizeAddressCore(baseline.getPropertyAddress());
        boolean addressMatches = !extractedAddress.isEmpty() && !baselineAddress.isEmpty()
                && (extractedAddress.contains(baselineAddress) || baselineAddress.contains(extractedAddress));
        return ownerMatches && addressMatches;
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "");
    }

    // 등기부 소재지번엔 "외 N필지"가 붙고, 중개대상물 소재지엔 "OO호" 동/호수가 붙는 등
    // 같은 부동산이라도 서로 다른 꼬리표가 붙어 문자열이 갈릴 수 있어, 비교 전에 그 꼬리표를 떼어낸다.
    private String normalizeAddressCore(String value) {
        String v = normalize(value);
        v = v.replaceAll("외\\d+필지$", "");
        v = v.replaceAll("\\d+동\\d+호$", "");
        v = v.replaceAll("\\d+호$", "");
        return v;
    }

    private List<HighlightRegion> searchRegions(Long userId, Long ocrResultId, String keyword,
                                                 boolean firstMatchOnly) throws IOException {
        KeywordSearchResponse response = ocrService.searchKeyword(userId, ocrResultId, keyword);
        List<KeywordSearchResponse.MatchResult> matches = response.matches();
        if (matches.isEmpty()) {
            return List.of();
        }
        if (firstMatchOnly) {
            matches = matches.subList(0, 1);
        }

        List<HighlightRegion> regions = new ArrayList<>();
        for (KeywordSearchResponse.MatchResult match : matches) {
            toRegion(match, keyword).ifPresent(regions::add);
        }
        return regions;
    }

    // vertices는 OcrService.mergeVertices가 축정렬 직사각형으로 병합해둔 상태(0=좌상, 2=우하)이므로
    // 이 두 점만으로 CLOVA 픽셀 좌표 기준 박스를 그대로 만든다(정규화하지 않음).
    private Optional<HighlightRegion> toRegion(KeywordSearchResponse.MatchResult match, String label) {
        List<KeywordSearchResponse.Vertex> vertices = match.vertices();
        if (vertices == null || vertices.size() < 3) {
            return Optional.empty();
        }
        KeywordSearchResponse.Vertex topLeft = vertices.get(0);
        KeywordSearchResponse.Vertex bottomRight = vertices.get(2);

        BoundingBox box = new BoundingBox(
                topLeft.x(), topLeft.y(),
                bottomRight.x() - topLeft.x(), bottomRight.y() - topLeft.y());

        return Optional.of(new HighlightRegion(match.pageIndex(), box, label));
    }

    private String toJson(BrokerageDocumentAnalysisResponse result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }
}