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
import java.util.stream.Collectors;

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
                if (image.fields() == null || image.fields().isEmpty()) continue;

                // lineBreak 기준으로 필드를 줄 단위로 묶기
                List<List<ClovaOcrResponse.Field>> lines = new ArrayList<>();
                List<ClovaOcrResponse.Field> currentLine = new ArrayList<>();
                for (ClovaOcrResponse.Field field : image.fields()) {
                    currentLine.add(field);
                    if (Boolean.TRUE.equals(field.lineBreak())) {
                        lines.add(currentLine);
                        currentLine = new ArrayList<>();
                    }
                }
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine);
                }

                // 줄 단위로 합친 텍스트에서 키워드 검색
                for (List<ClovaOcrResponse.Field> line : lines) {
                    String lineText = line.stream()
                            .map(f -> f.inferText() != null ? f.inferText() : "")
                            .collect(Collectors.joining(" "))
                            .trim();
                    if (lineText.contains(keyword)) {
                        matches.add(new KeywordSearchResponse.MatchResult(
                                pageIndex, lineText, mergeVertices(line)));
                    }
                }
            }
        }

        return new KeywordSearchResponse(keyword, matches.size(), matches);
    }

    // 여러 필드의 bounding box를 하나로 합치기 (min/max 좌표)
    private List<KeywordSearchResponse.Vertex> mergeVertices(List<ClovaOcrResponse.Field> fields) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

        for (ClovaOcrResponse.Field field : fields) {
            if (field.boundingPoly() == null || field.boundingPoly().vertices() == null) continue;
            for (ClovaOcrResponse.Vertex v : field.boundingPoly().vertices()) {
                if (v.x() != null) { minX = Math.min(minX, v.x()); maxX = Math.max(maxX, v.x()); }
                if (v.y() != null) { minY = Math.min(minY, v.y()); maxY = Math.max(maxY, v.y()); }
            }
        }

        if (minX == Double.MAX_VALUE) return List.of();

        return List.of(
                new KeywordSearchResponse.Vertex(minX, minY), // top-left
                new KeywordSearchResponse.Vertex(maxX, minY), // top-right
                new KeywordSearchResponse.Vertex(maxX, maxY), // bottom-right
                new KeywordSearchResponse.Vertex(minX, maxY)  // bottom-left
        );
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
