package com.example.vivizip.common.exception.auth;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;

public class InvalidTokenException extends GeneralException {
    public InvalidTokenException() {
        super(ErrorStatus.AUTH_INVALID_TOKEN);
    }
}
