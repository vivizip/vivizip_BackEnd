package com.example.vivizip.common.exception.auth;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;

public class RefreshTokenNotFoundException extends GeneralException {
    public RefreshTokenNotFoundException() {
        super(ErrorStatus.AUTH_REFRESH_TOKEN_NOT_FOUND);
    }
}
