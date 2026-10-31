package com.marketplace.service;

import com.marketplace.auth.OtpProperties;
import com.marketplace.domain.UserEntity;
import com.marketplace.repository.UserRepository;
import com.marketplace.web.dto.AuthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private final SecureRandom random = new SecureRandom();
    private final Map<String, OtpEntry> codes = new ConcurrentHashMap<>();

    private final OtpProperties properties;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final MailService mailService;

    public OtpService(
            OtpProperties properties,
            UserRepository userRepository,
            AuthService authService,
            MailService mailService
    ) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.authService = authService;
        this.mailService = mailService;
    }

    public void requestCode(String email) {
        String normalized = AuthService.normalizeEmail(email);
        userRepository.findByEmailIgnoreCase(normalized)
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String code = String.format("%06d", random.nextInt(1_000_000));
        Instant expiresAt = Instant.now().plusSeconds(properties.getTtlSeconds());
        codes.put(normalized, new OtpEntry(code, expiresAt, 0));
        mailService.sendOtpCode(normalized, code);
    }

    public AuthResponse verifyCode(String email, String code) {
        String normalized = AuthService.normalizeEmail(email);
        OtpEntry entry = codes.get(normalized);
        if (entry == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired code");
        }
        if (Instant.now().isAfter(entry.expiresAt)) {
            codes.remove(normalized);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired code");
        }
        if (entry.attempts >= properties.getMaxAttempts()) {
            codes.remove(normalized);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Too many attempts");
        }
        if (!entry.code.equals(code.trim())) {
            codes.put(normalized, entry.withAttempts(entry.attempts + 1));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired code");
        }

        codes.remove(normalized);
        return authService.issueTokenForEmail(normalized);
    }

    private record OtpEntry(String code, Instant expiresAt, int attempts) {
        OtpEntry withAttempts(int newAttempts) {
            return new OtpEntry(code, expiresAt, newAttempts);
        }
    }
}
