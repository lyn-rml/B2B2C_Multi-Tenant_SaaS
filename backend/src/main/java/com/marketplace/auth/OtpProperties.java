package com.marketplace.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.otp")
public class OtpProperties {
    private int ttlSeconds = 600;
    private int maxAttempts = 5;
    private String fromEmail = "noreply@student-marketplace.local";
}
