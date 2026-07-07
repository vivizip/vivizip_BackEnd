package com.example.vivizip.common.exception;

public class InternalServerException extends GeneralException {
    public InternalServerException() {
        super(ErrorStatus._INTERNAL_SERVER_ERROR);
    }
}
