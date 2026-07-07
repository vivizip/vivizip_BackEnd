package com.example.vivizip.user.controller;

import com.example.vivizip.user.dto.OptionResponse;
import com.example.vivizip.user.entity.Gender;
import com.example.vivizip.user.entity.Language;
import com.example.vivizip.user.entity.Nationality;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Tag(name = "Options", description = "온보딩 선택지 조회 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/options")
@RequiredArgsConstructor
public class OptionController {

    @Operation(summary = "언어 목록 조회")
    @GetMapping("/languages")
    public ResponseEntity<List<OptionResponse>> getLanguages() {
        List<OptionResponse> options = Arrays.stream(Language.values())
                .map(e -> new OptionResponse(e.name(), e.getLabel()))
                .toList();
        return ResponseEntity.ok(options);
    }

    @Operation(summary = "국적 목록 조회")
    @GetMapping("/nationalities")
    public ResponseEntity<List<OptionResponse>> getNationalities() {
        List<OptionResponse> options = Arrays.stream(Nationality.values())
                .map(e -> new OptionResponse(e.name(), e.getLabel()))
                .toList();
        return ResponseEntity.ok(options);
    }

    @Operation(summary = "성별 목록 조회")
    @GetMapping("/genders")
    public ResponseEntity<List<OptionResponse>> getGenders() {
        List<OptionResponse> options = Arrays.stream(Gender.values())
                .map(e -> new OptionResponse(e.name(), e.getLabel()))
                .toList();
        return ResponseEntity.ok(options);
    }
}
