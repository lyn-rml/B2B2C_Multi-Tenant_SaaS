package com.marketplace.service;

import com.marketplace.auth.JwtService;
import com.marketplace.domain.*;
import com.marketplace.repository.UserRepository;
import com.marketplace.repository.UserRoleRepository;
import com.marketplace.web.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository users;
    private final UserRoleRepository roles;
    private final PasswordEncoder passwords;
    private final JwtService jwt;

    public AuthService(UserRepository users, UserRoleRepository roles, PasswordEncoder passwords, JwtService jwt) {
        this.users = users;
        this.roles = roles;
        this.passwords = passwords;
        this.jwt = jwt;
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.role() != RoleType.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Public registration only grants STUDENT");
        }
        if (request.tenantId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "STUDENT must not have a tenantId");
        }
        String email = normalizeEmail(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(passwords.encode(request.password()));
        user.setActive(true);
        user = users.save(user);
        UserRoleEntity role = new UserRoleEntity();
        role.setUser(user);
        role.setRole(RoleType.STUDENT);
        roles.save(role);
        user.getRoles().add(role);
        return response(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserEntity user = activeUser(request.email());
        if (!passwords.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return response(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse issueTokenForEmail(String email) {
        return response(activeUser(email));
    }

    @Transactional(readOnly = true)
    public AuthResponse me(Long userId) {
        UserEntity user = users.findWithRolesById(userId).filter(UserEntity::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        return new AuthResponse(null, user.getEmail(), user.getFullName(), JwtService.claimsFromUser(user));
    }

    private UserEntity activeUser(String email) {
        return users.findByEmailIgnoreCase(normalizeEmail(email)).filter(UserEntity::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    }

    private AuthResponse response(UserEntity user) {
        return new AuthResponse(jwt.generateToken(user), user.getEmail(), user.getFullName(), JwtService.claimsFromUser(user));
    }
}
