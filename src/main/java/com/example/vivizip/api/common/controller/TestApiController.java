package com.example.vivizip.api.common.controller;

import com.example.vivizip.api.common.dto.ApiResponseDto;
import com.example.vivizip.common.annotation.DisableSwaggerSecurity;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "테스트 용 API")
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestApiController {

    @DisableSwaggerSecurity // 인증 관련 잠금 없애주는 annotation
    @GetMapping("/health-check")
    public ApiResponseDto<String> healthCheckup() {
        return ApiResponseDto.onSuccess(HttpStatus.OK.toString());
    }

}
