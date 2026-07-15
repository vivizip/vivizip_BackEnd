package com.example.vivizip.ocr.service;

import com.example.vivizip.ocr.client.ClovaOcrClient;
import com.example.vivizip.ocr.dto.ClovaOcrRequest;
import com.example.vivizip.ocr.dto.ClovaOcrResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OcrTextExtractionService {

    private final ClovaOcrClient clovaOcrClient;

    public ClovaOcrResponse requestOcr(MultipartFile file) throws IOException {
        String base64 = Base64.getEncoder().encodeToString(file.getBytes());
        String format = extractFormat(file.getOriginalFilename());
        ClovaOcrRequest request = ClovaOcrRequest.ofSingleImage(format, base64);
        return clovaOcrClient.callOcr(request);
    }

    public String extractText(List<MultipartFile> files) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < files.size(); i++) {
            ClovaOcrResponse response = requestOcr(files.get(i));

            if (files.size() > 1) {
                sb.append("=== 페이지 ").append(i + 1).append(" ===\n");
            }
            response.images().forEach(image ->
                    image.fields().forEach(field -> {
                        sb.append(field.inferText());
                        sb.append(Boolean.TRUE.equals(field.lineBreak()) ? "\n" : " ");
                    })
            );
            if (i < files.size() - 1) {
                sb.append("\n\n");
            }
        }
        return sb.toString().trim();
    }

    private String extractFormat(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
