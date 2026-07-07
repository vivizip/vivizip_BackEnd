package com.example.vivizip.consts;

public class StaticVariable {
    public static final String SWAGGER_JWT = "JWT";
    public static final String SWAGGER_BEARER = "Bearer";
    public static final String AUTHORIZATION = "Authorization";

    public static final long JWT_ACCESS_EXPIRATION = 1800000L; // 30분
    public static final long JWT_REFRESH_EXPIRATION = 1209600000L; // 14일
}
