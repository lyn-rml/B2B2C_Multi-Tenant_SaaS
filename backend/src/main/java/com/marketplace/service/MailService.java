package com.marketplace.service;

import com.marketplace.auth.OtpProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final OtpProperties otpProperties;

    public MailService(JavaMailSender mailSender, OtpProperties otpProperties) {
        this.mailSender = mailSender;
        this.otpProperties = otpProperties;
    }

    public void sendOtpCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(otpProperties.getFromEmail());
        message.setTo(toEmail);
        message.setSubject("Your login code");
        message.setText("Your one-time login code is: " + code + "\n\nIt expires in "
                + otpProperties.getTtlSeconds() / 60 + " minutes.");
        mailSender.send(message);
    }
}
