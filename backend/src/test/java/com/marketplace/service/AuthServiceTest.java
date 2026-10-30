package com.marketplace.service;

import com.marketplace.auth.JwtService;
import com.marketplace.domain.RoleType;
import com.marketplace.repository.*;
import com.marketplace.web.dto.RegisterRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    @ParameterizedTest
    @EnumSource(value = RoleType.class, names = "STUDENT", mode = EnumSource.Mode.EXCLUDE)
    void publicRegistrationCannotGrantPrivilegedRoles(RoleType role) {
        var users = mock(UserRepository.class);
        var roles = mock(UserRoleRepository.class);
        var service = new AuthService(users, mock(TenantRepository.class),
                mock(TenantMembershipRepository.class), roles, mock(PasswordEncoder.class), mock(JwtService.class));
        var request = new RegisterRequest("person@example.test", "A person", "unused-password", role, UUID.randomUUID());
        var error = assertThrows(ResponseStatusException.class, () -> service.register(request));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(users, never()).save(any());
        verifyNoInteractions(roles);
    }
}
