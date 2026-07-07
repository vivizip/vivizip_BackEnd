package com.example.vivizip.common.exception.auth;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;

public class InvalidKakaoTokenException extends GeneralException {
    public InvalidKakaoTokenException() {
        super(ErrorStatus.AUTH_INVALID_AUTH_CODE);
    }
}
