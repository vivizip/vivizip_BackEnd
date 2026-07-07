package com.example.vivizip.common.exception.auth;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;

public class UnauthorizedAccessException extends GeneralException {
    public UnauthorizedAccessException() {
        super(ErrorStatus.AUTH_MUST_AUTHORIZED_URI);
    }
}
