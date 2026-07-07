package com.example.vivizip.common.exception.auth;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;

public class KakaoEmailNotFoundException extends GeneralException {
    public KakaoEmailNotFoundException() {
        super(ErrorStatus.AUTH_OAUTH2_EMAIL_NOT_FOUND_FROM_PROVIDER);
    }
}
