package com.example.vivizip.common.annotation;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.PredefinedErrorStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.example.vivizip.common.exception.PredefinedErrorStatus.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorCodeExample {
    ErrorStatus[] value() default {ErrorStatus._INTERNAL_SERVER_ERROR};

    PredefinedErrorStatus status() default DEFAULT;
}
