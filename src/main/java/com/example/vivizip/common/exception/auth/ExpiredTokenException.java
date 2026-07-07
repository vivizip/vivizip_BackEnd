package com.example.vivizip.common.exception.auth;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;

public class ExpiredTokenException extends GeneralException {
    public ExpiredTokenException() {
        super(ErrorStatus.AUTH_TOKEN_HAS_EXPIRED);
    }
}
