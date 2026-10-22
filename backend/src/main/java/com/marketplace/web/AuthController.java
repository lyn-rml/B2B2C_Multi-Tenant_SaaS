package com.marketplace.web;

import com.marketplace.service.AuthService;
import com.marketplace.service.OtpService;
import com.marketplace.web.dto.AuthResponse;
import com.marketplace.web.dto.LoginRequest;
import com.marketplace.web.dto.OtpRequest;
import com.marketplace.web.dto.OtpVerifyRequest;
import com.marketplace.web.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    public AuthController(AuthService authService, OtpService otpService) {
        this.authService = authService;
        this.otpService = otpService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Current user profile and roles")
    public AuthResponse me(Authentication authentication) {
        return authService.me(Long.parseLong(authentication.getName()));
    }

    @PostMapping("/otp/request")
    @Operation(summary = "Request a one-time login code by email")
    public void requestOtp(@Valid @RequestBody OtpRequest request) {
        otpService.requestCode(request.email());
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Verify email OTP and receive JWT")
    public AuthResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return otpService.verifyCode(request.email(), request.code());
    }
}
