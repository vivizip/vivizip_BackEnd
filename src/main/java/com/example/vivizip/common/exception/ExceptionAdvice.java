package com.example.vivizip.common.exception;

import com.example.vivizip.api.common.dto.ApiResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ExceptionAdvice {

    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponseDto<?>> handleGeneralException(GeneralException e) {
        Reason reason = e.getErrorReasonHttpStatus();
        log.warn("GeneralException: [{}] {}", reason.getCode(), reason.getMessage());
        return ResponseEntity
                .status(reason.getHttpStatus())
                .body(ApiResponseDto.onFailure(reason.getCode(), reason.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDto<?>> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = fieldError != null ? fieldError.getDefaultMessage() : ErrorStatus._BAD_REQUEST.getMessage();
        log.warn("ValidationException: {}", message);
        return ResponseEntity
                .badRequest()
                .body(ApiResponseDto.onFailure(ErrorStatus._BAD_REQUEST.getCode(), message, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<?>> handleException(Exception e) {
        log.error("UnhandledException: {}", e.getMessage(), e);
        Reason reason = ErrorStatus._INTERNAL_SERVER_ERROR.getReasonHttpStatus();
        return ResponseEntity
                .status(reason.getHttpStatus())
                .body(ApiResponseDto.onFailure(reason.getCode(), reason.getMessage(), null));
    }
}
