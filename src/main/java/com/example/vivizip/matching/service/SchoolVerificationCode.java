package com.example.vivizip.matching.service;

record SchoolVerificationCode(
        Long schoolId,
        String schoolEmail,
        String code
) {}