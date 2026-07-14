package com.example.vivizip.matching.service;

public interface EmailService {
    void sendVerificationCode(String email, String code);
}