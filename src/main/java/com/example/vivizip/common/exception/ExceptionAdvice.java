package com.example.vivizip.common.exception;

import com.example.vivizip.api.common.dto.ApiResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionAdvice {

    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponseDto<?>> handleGeneralException(
            GeneralException e
    ) {

        Reason reason = e.getErrorReasonHttpStatus();

        return ResponseEntity
                .status(reason.getHttpStatus())
                .body(ApiResponseDto.onFailure(
                        reason.getCode(),
                        reason.getMessage(),
                        null
                ));
    }
}
