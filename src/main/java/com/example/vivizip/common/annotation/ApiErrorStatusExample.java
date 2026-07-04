package com.example.vivizip.common.annotation;

import com.example.vivizip.common.exception.BaseErrorCode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorStatusExample {
    Class<? extends BaseErrorCode> value();
}