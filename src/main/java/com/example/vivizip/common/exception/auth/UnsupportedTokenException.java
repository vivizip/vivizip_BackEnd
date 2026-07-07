package com.example.vivizip.common.exception.auth;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;

public class UnsupportedTokenException extends GeneralException {
    public UnsupportedTokenException() {
        super(ErrorStatus.AUTH_TOKEN_IS_UNSUPPORTED);
    }
}
