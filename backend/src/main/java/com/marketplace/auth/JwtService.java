package com.marketplace.auth;

import com.marketplace.domain.UserEntity;
import com.marketplace.domain.UserRoleEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UserEntity user) {
        List<RoleClaim> roles = user.getRoles().stream()
                .map(this::toClaim)
                .toList();

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("fullName", user.getFullName())
                .claim("roles", roles.stream()
                        .map(r -> r.role().name() + ":" + (r.tenantId() != null ? r.tenantId() : ""))
                        .toList())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + properties.getExpirationMs()))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    private RoleClaim toClaim(UserRoleEntity entity) {
        if (entity.getTenant() == null) {
            return new RoleClaim(entity.getRole(), null, null);
        }
        return new RoleClaim(
                entity.getRole(),
                entity.getTenant().getId(),
                entity.getTenant().getName()
        );
    }

    public static List<RoleClaim> claimsFromUser(UserEntity user) {
        return user.getRoles().stream()
                .map(r -> {
                    if (r.getTenant() == null) {
                        return new RoleClaim(r.getRole(), null, null);
                    }
                    return new RoleClaim(r.getRole(), r.getTenant().getId(), r.getTenant().getName());
                })
                .toList();
    }

}
