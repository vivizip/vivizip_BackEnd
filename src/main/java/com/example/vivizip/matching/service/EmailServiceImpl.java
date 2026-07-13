package com.example.vivizip.matching.service;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendVerificationCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("[Vivizip] 학교 이메일 인증 코드");
        message.setText("인증 코드: " + code + "\n인증 코드는 5분간 유효합니다.");

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.warn("[학교 이메일 인증] {}로 인증 코드 발송 실패: {}", email, e.getMessage());
            throw new GeneralException(ErrorStatus.SCHOOL_VERIFICATION_EMAIL_SEND_FAILED);
        }
        log.info("[학교 이메일 인증] {}로 인증 코드 발송 완료", email);
    }
}