package com.example.vivizip.common.exception.auth;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;

public class NullTokenException extends GeneralException {
    public NullTokenException() {
        super(ErrorStatus.AUTH_IS_NULL);
    }
}
