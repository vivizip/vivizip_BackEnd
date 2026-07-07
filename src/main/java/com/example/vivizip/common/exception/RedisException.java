package com.example.vivizip.common.exception;

public class RedisException extends GeneralException {
    public RedisException() {
        super(ErrorStatus.REDIS_ERROR);
    }
}
