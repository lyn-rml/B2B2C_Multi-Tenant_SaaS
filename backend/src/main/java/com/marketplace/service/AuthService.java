package com.marketplace.service;

import com.marketplace.auth.JwtService;
import com.marketplace.auth.RoleClaim;
import com.marketplace.domain.*;
import com.marketplace.repository.TenantMembershipRepository;
import com.marketplace.repository.TenantRepository;
import com.marketplace.repository.UserRepository;
import com.marketplace.repository.UserRoleRepository;
import com.marketplace.web.dto.AuthResponse;
import com.marketplace.web.dto.LoginRequest;
import com.marketplace.web.dto.RegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            TenantMembershipRepository membershipRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        validateRoleAndTenant(request.role(), request.tenantId());

        UserEntity user = new UserEntity();
        user.setEmail(request.email().toLowerCase());
        user.setFullName(request.fullName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setActive(true);
        user = userRepository.save(user);

        if (request.role() == RoleType.STUDENT) {
            attachRole(user, null, RoleType.STUDENT);
        } else {
            TenantEntity tenant = tenantRepository.findById(request.tenantId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown tenant"));
            attachMembership(user, tenant);
            attachRole(user, tenant, request.role());
        }

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse issueTokenForEmail(String email) {
        UserEntity user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        user = userRepository.findWithRolesById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse me(Long userId) {
        UserEntity user = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return new AuthResponse(
                null,
                user.getEmail(),
                user.getFullName(),
                JwtService.claimsFromUser(user)
        );
    }

    private AuthResponse buildAuthResponse(UserEntity user) {
        String token = jwtService.generateToken(user);
        return new AuthResponse(
                token,
                user.getEmail(),
                user.getFullName(),
                JwtService.claimsFromUser(user)
        );
    }

    private void validateRoleAndTenant(RoleType role, java.util.UUID tenantId) {
        if (role != RoleType.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Public registration only grants STUDENT; staff roles require an administrator");
        }
        if (role == RoleType.STUDENT && tenantId != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "STUDENT role must not have tenantId");
        }
        if (role != RoleType.STUDENT && tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant id required for staff roles");
        }
    }

    private void attachMembership(UserEntity user, TenantEntity tenant) {
        TenantMembershipEntity membership = new TenantMembershipEntity();
        membership.setUser(user);
        membership.setTenant(tenant);
        membershipRepository.save(membership);
    }

    private void attachRole(UserEntity user, TenantEntity tenant, RoleType role) {
        if (role == RoleType.TENANT_ADMIN) {
            ensureSingleTenantAdminScope(user.getId());
        }
        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUser(user);
        userRole.setTenant(tenant);
        userRole.setRole(role);
        userRoleRepository.save(userRole);
    }

    /**
     * A user may be TENANT_ADMIN for at most one supermarket chain.
     */
    private void ensureSingleTenantAdminScope(Long userId) {
        if (userRoleRepository.existsByUser_IdAndRole(userId, RoleType.TENANT_ADMIN)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A user can be TENANT_ADMIN for only one chain"
            );
        }
    }
}
