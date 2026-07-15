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
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaseDocumentUploadService {

    private final S3Service s3Service;
    private final OcrTextExtractionService ocrTextExtractionService;
    private final DocumentAnalysisService documentAnalysisService;
    private final LeaseDocumentUploadRecorder recorder;

    public BuildingLedgerAnalysisResponse uploadAndAnalyzeBuildingLedger(Long userId, Long leaseCaseId, List<MultipartFile> files) throws IOException {
        return BuildingLedgerAnalysisResponse.from(
                uploadAndAnalyze(userId, leaseCaseId, LeaseDocumentType.BUILDING_LEDGER, files));
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
        List<String> keys = files.stream()
                .map(file -> s3Service.uploadPrivate(file, S3Folder.LEASE_DOCUMENT.getPath()))
                .toList();
        return recorder.saveUploadedDocument(leaseCaseId, documentType, keys);
    }
}
