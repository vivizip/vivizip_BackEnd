package com.example.vivizip.ocr.service;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.ocr.client.ClovaOcrClient;
import com.example.vivizip.ocr.dto.ClovaOcrRequest;
import com.example.vivizip.ocr.dto.ClovaOcrResponse;
import com.example.vivizip.ocr.dto.KeywordSearchResponse;
import com.example.vivizip.ocr.dto.OcrSaveResponse;
import com.example.vivizip.ocr.entity.OcrResult;
import com.example.vivizip.ocr.repository.OcrResultRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OcrService {

    private final ClovaOcrClient clovaOcrClient;
    private final OcrResultRepository ocrResultRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OcrSaveResponse save(Long userId, List<MultipartFile> files) throws IOException {
        List<ClovaOcrResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            responses.add(requestOcr(file));
        }
        String rawJson = objectMapper.writeValueAsString(responses);
        OcrResult result = ocrResultRepository.save(OcrResult.create(userId, rawJson));
        return OcrSaveResponse.of(result, responses.size());
    }

    @Transactional(readOnly = true)
    public KeywordSearchResponse searchKeyword(Long userId, Long id, String keyword) throws IOException {
        OcrResult result = ocrResultRepository.findById(id)
                .orElseThrow(() -> new GeneralException(ErrorStatus.OCR_RESULT_NOT_FOUND));
        if (!result.getUserId().equals(userId)) {
            throw new GeneralException(ErrorStatus.OCR_RESULT_FORBIDDEN);
        }

        List<ClovaOcrResponse> responses = objectMapper.readValue(
                result.getRawJson(), new TypeReference<>() {});

        List<KeywordSearchResponse.MatchResult> matches = new ArrayList<>();
        for (int pageIndex = 0; pageIndex < responses.size(); pageIndex++) {
            ClovaOcrResponse response = responses.get(pageIndex);
            for (ClovaOcrResponse.Image image : response.images()) {
                if (image.fields() == null) continue;
                for (ClovaOcrResponse.Field field : image.fields()) {
                    if (field.inferText() != null && field.inferText().contains(keyword)) {
                        List<KeywordSearchResponse.Vertex> vertices = List.of();
                        if (field.boundingPoly() != null && field.boundingPoly().vertices() != null) {
                            vertices = field.boundingPoly().vertices().stream()
                                    .map(v -> new KeywordSearchResponse.Vertex(v.x(), v.y()))
                                    .toList();
                        }
                        matches.add(new KeywordSearchResponse.MatchResult(pageIndex, field.inferText(), vertices));
                    }
                }
            }
        }

        return new KeywordSearchResponse(keyword, matches.size(), matches);
    }

    private ClovaOcrResponse requestOcr(MultipartFile file) throws IOException {
        String base64 = Base64.getEncoder().encodeToString(file.getBytes());
        String format = extractFormat(file.getOriginalFilename());
        ClovaOcrRequest request = ClovaOcrRequest.ofSingleImage(format, base64);
        return clovaOcrClient.callOcr(request);
    }

    private String extractFormat(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
