package com.example.vivizip.ocr.controller;

import com.example.vivizip.ocr.client.ClovaOcrClient;
import com.example.vivizip.ocr.dto.ClovaOcrRequest;
import com.example.vivizip.ocr.dto.ClovaOcrResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Tag(name = "OCR", description = "CLOVA OCR API 엔드포인트")
@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    private final ClovaOcrClient clovaOcrClient;

    public OcrController(ClovaOcrClient clovaOcrClient) {
        this.clovaOcrClient = clovaOcrClient;
    }

    @Operation(
            summary = "OCR 원본 응답 반환",
            description = "이미지를 1장 이상 업로드하면 각 페이지의 CLOVA OCR API 응답을 리스트로 반환합니다. 응답 구조 확인용입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OCR 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 파일 형식")
    })
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<List<ClovaOcrResponse>> ocr(
            @Parameter(description = "OCR 처리할 이미지 파일 목록 (jpg, png 등, 여러 장 가능)", required = true)
            @RequestParam("files") List<MultipartFile> files
    ) throws IOException {
        List<ClovaOcrResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            responses.add(requestOcr(file));
        }
        return ResponseEntity.ok(responses);
    }

    @Operation(
            summary = "OCR 텍스트 추출",
            description = "이미지를 1장 이상 업로드하면 인식된 텍스트를 페이지 구분자(=== 페이지 N ===)와 함께 하나의 문자열로 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "텍스트 추출 성공",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(type = "string", example = "=== 페이지 1 ===\n안녕하세요 반갑습니다"))),
            @ApiResponse(responseCode = "400", description = "잘못된 파일 형식")
    })
    @PostMapping(value = "/text", consumes = "multipart/form-data")
    public ResponseEntity<String> ocrText(
            @Parameter(description = "OCR 처리할 이미지 파일 목록 (jpg, png 등, 여러 장 가능)", required = true)
            @RequestParam("files") List<MultipartFile> files
    ) throws IOException {
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
        return ResponseEntity.ok(sb.toString().trim());
    }

    @Operation(
            summary = "OCR 표 추출",
            description = "이미지를 1장 이상 업로드하면 각 페이지에서 감지된 표(Table)의 셀 내용을 [행,열] 형식으로 반환합니다. 표가 없을 경우 '표 없음' 메시지를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "표 추출 성공",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(type = "string", example = "=== 페이지 1 ===\n[0,0] 항목\n[0,1] 금액\n---"))),
            @ApiResponse(responseCode = "400", description = "잘못된 파일 형식")
    })
    @PostMapping(value = "/table", consumes = "multipart/form-data")
    public ResponseEntity<String> ocrTable(
            @Parameter(description = "OCR 처리할 이미지 파일 목록 (jpg, png 등, 여러 장 가능)", required = true)
            @RequestParam("files") List<MultipartFile> files
    ) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < files.size(); i++) {
            ClovaOcrResponse response = requestOcr(files.get(i));

            if (files.size() > 1) {
                sb.append("=== 페이지 ").append(i + 1).append(" ===\n");
            }
            response.images().forEach(image -> {
                if (image.tables() == null) {
                    sb.append("표 없음 (표 추출 안 켜졌거나 표 미검출)\n");
                    return;
                }
                image.tables().forEach(table -> {
                    // 셀을 행 → 열 순서로 정렬해서 표 복원
                    table.cells().stream()
                            .sorted((a, b) -> {
                                int r = Integer.compare(a.rowIndex(), b.rowIndex());
                                return r != 0 ? r : Integer.compare(a.columnIndex(), b.columnIndex());
                            })
                            .forEach(cell -> {
                                String cellText = cell.cellTextLines() == null ? "" :
                                        cell.cellTextLines().stream()
                                                .flatMap(line -> line.cellWords().stream())
                                                .map(ClovaOcrResponse.CellWord::inferText)
                                                .reduce("", (x, y) -> x + " " + y).trim();
                                sb.append("[")
                                        .append(cell.rowIndex()).append(",").append(cell.columnIndex())
                                        .append("] ").append(cellText).append("\n");
                            });
                    sb.append("---\n");
                });
            });
            if (i < files.size() - 1) {
                sb.append("\n");
            }
        }
        return ResponseEntity.ok(sb.toString());
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
