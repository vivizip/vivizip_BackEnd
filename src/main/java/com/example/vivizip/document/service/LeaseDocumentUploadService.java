package com.example.vivizip.document.service;

import com.example.vivizip.S3.enums.S3Folder;
import com.example.vivizip.S3.service.S3Service;
import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.document.dto.BuildingLedgerAnalysisResponse;
import com.example.vivizip.document.dto.DocumentAnalysisResponse;
import com.example.vivizip.document.entity.LeaseDocument;
import com.example.vivizip.document.entity.LeaseDocumentType;
import com.example.vivizip.ocr.service.OcrTextExtractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaseDocumentUploadService {

    private final S3Service s3Service;
    private final OcrTextExtractionService ocrTextExtractionService;
    private final DocumentAnalysisService documentAnalysisService;
    private final LeaseDocumentUploadRecorder recorder;

    // 건축물대장은 1쪽(표제부)에 분석에 필요한 정보가 다 있고, 뒷장 변동사항(이력)에는
    // "위법건축물 표기/해제" 같은 과거 이력 문구가 섞여 있어 hasViolation 같은 판단을 흐릴 수 있어서
    // 여러 장이 아니라 1장만 받는다.
    public BuildingLedgerAnalysisResponse uploadAndAnalyzeBuildingLedger(Long userId, Long leaseCaseId, MultipartFile file) throws IOException {
        return BuildingLedgerAnalysisResponse.from(
                uploadAndAnalyze(userId, leaseCaseId, LeaseDocumentType.BUILDING_LEDGER, List.of(file)));
    }

    // 서류 타입별 결과 DTO가 다 달라서 컨트롤러는 타입별 엔드포인트로 분리하되,
    // 업로드+OCR+분석 오케스트레이션 자체는 여기서 공통으로 재사용한다.
    private DocumentAnalysisResponse uploadAndAnalyze(Long userId, Long leaseCaseId, LeaseDocumentType documentType, List<MultipartFile> files) throws IOException {
        LeaseDocument document = uploadFiles(userId, leaseCaseId, documentType, files);
        String ocrText = ocrTextExtractionService.extractText(files);
        return documentAnalysisService.analyze(userId, document.getId(), ocrText);
    }

    private LeaseDocument uploadFiles(Long userId, Long leaseCaseId, LeaseDocumentType documentType, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new GeneralException(ErrorStatus.DOCUMENT_FILE_EMPTY);
        }
        recorder.assertOwnedLeaseCase(userId, leaseCaseId);

        List<String> uploadedKeys = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                uploadedKeys.add(s3Service.uploadPrivate(file, S3Folder.LEASE_DOCUMENT.getPath()));
            }
            return recorder.saveUploadedDocument(leaseCaseId, documentType, uploadedKeys);
        } catch (RuntimeException e) {
            uploadedKeys.forEach(s3Service::delete);
            throw e;
        }
    }
}
